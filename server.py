#!/usr/bin/env python3
"""SuperCopy local server — serves index.html + URL expansion API (no CORS issues)."""
import http.server
import socketserver
import urllib.request
import urllib.parse
import urllib.error
import http.cookiejar
import json
import sys
import os
import gzip
import io

PORT = 8080
HOST = '0.0.0.0'

# Module-level cookie-enabled opener (shared across requests)
_cookie_jar = http.cookiejar.CookieJar()
_opener = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(_cookie_jar),
    urllib.request.HTTPHandler,
    urllib.request.HTTPSHandler,
    urllib.request.HTTPRedirectHandler,
)

# Domains known to trigger anti-bot / captcha pages
_CAPTCHA_DOMAINS = [
    'website-login',
    'captcha',
    'verify.',
    '/login?',
    '/login/',
    'verify.xiaohongshu',
]

def _is_captcha_or_login(url):
    """Return True if the URL looks like a captcha / login page."""
    url_lower = url.lower()
    return any(p in url_lower for p in _CAPTCHA_DOMAINS)

def _decode_body(resp):
    """Decode response body, handling gzip when present."""
    body = resp.read()
    encoding = resp.headers.get('Content-Encoding', '')
    if 'gzip' in encoding:
        try:
            body = gzip.decompress(body)
        except Exception:
            pass
    charset = resp.headers.get_content_charset() or 'utf-8'
    return body.decode(charset, errors='ignore')

class SuperCopyHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith('/expand?'):
            self.expand_url()
        else:
            super().do_GET()

    def expand_url(self):
        """Follow redirects for a short URL and return the final URL as JSON."""
        parsed = urllib.parse.urlparse(self.path)
        params = urllib.parse.parse_qs(parsed.query)
        url = params.get('url', [None])[0]
        if not url:
            self.send_json({'error': 'Missing ?url= parameter'}, 400)
            return

        def make_request(u, method='GET'):
            req = urllib.request.Request(u, method=method)
            # Mimic a real Chrome browser as closely as possible
            req.add_header('User-Agent',
                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
                'AppleWebKit/537.36 (KHTML, like Gecko) '
                'Chrome/131.0.0.0 Safari/537.36'
            )
            req.add_header('Accept',
                'text/html,application/xhtml+xml,application/xml;'
                'q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8'
            )
            req.add_header('Accept-Language', 'zh-CN,zh;q=0.9,en;q=0.8,en-US;q=0.7')
            req.add_header('Accept-Encoding', 'gzip, deflate, br')
            req.add_header('Cache-Control', 'no-cache')
            req.add_header('Pragma', 'no-cache')
            req.add_header('Sec-Ch-Ua',
                '"Google Chrome";v="131", "Chromium";v="131", "Not_A Brand";v="24"'
            )
            req.add_header('Sec-Ch-Ua-Mobile', '?0')
            req.add_header('Sec-Ch-Ua-Platform', '"Windows"')
            req.add_header('Sec-Fetch-Dest', 'document')
            req.add_header('Sec-Fetch-Mode', 'navigate')
            req.add_header('Sec-Fetch-Site', 'none')
            req.add_header('Sec-Fetch-User', '?1')
            req.add_header('Upgrade-Insecure-Requests', '1')
            req.add_header('Dnt', '1')
            return _opener.open(req, timeout=15)

        def extract_js_redirect(html):
            """Extract redirect URL from various embedding patterns."""
            import re
            # Pattern: <input id="target" value="https://...">  (reurl.cc)
            m = re.search(r"""<input[^>]+(?:id|name)\s*=\s*["']target["'][^>]+value\s*=\s*["'](https?://[^"']+)["']""", html)
            if m:
                return m.group(1)
            # Pattern: var url = 'https://...' or "https://..."  (m.tb.cn, etc.)
            m = re.search(r"var\s+url\s*=\s*['\"](https?://[^'\"]+)['\"]", html)
            if m:
                return m.group(1)
            # Pattern: window.location.href = '...' or window.location = '...'
            m = re.search(r"""(?:window\.location(?:\.href)?|location)\s*=\s*['\"](https?://[^'\"]+)['\"]""", html)
            if m:
                return m.group(1)
            # Pattern: meta refresh
            m = re.search(r"""<meta[^>]+http-equiv\s*=\s*["']refresh["'][^>]+content\s*=\s*["']\d+\s*;\s*url\s*=\s*(.+?)["'\s>]""", html)
            if m:
                target = m.group(1).strip().strip("'").strip('"')
                if target.startswith('http'):
                    return target
            return None

        def _body_looks_like_captcha(body_text):
            """Quick heuristic: does the response body look like a captcha / login page?"""
            if not body_text:
                return False
            lower = body_text.lower()
            indicators = [
                'captcha', '验证码', '滑块验证', '请完成安全验证',
                'please verify you are a human', 'are you a robot',
                'website-login', '请登录', '登录后查看',
                '访问受限', '请求太频繁', '过度访问',
                '_sec_verify',
            ]
            return sum(1 for s in indicators if s in lower) >= 1

        def follow_redirects(start_url, check_js=True):
            """Follow HTTP redirect chain and return (final_url, blocked).
            blocked=True means we hit a captcha/login wall."""
            final_url = start_url
            for method in ['GET', 'HEAD']:
                try:
                    resp = make_request(start_url, method)
                    resp_url = resp.geturl()

                    # Check if we've been redirected to a captcha / login page
                    if _is_captcha_or_login(resp_url):
                        resp.close()
                        return (start_url, True)  # blocked

                    if resp_url != start_url:
                        # HTTP redirect followed — recurse without JS extraction
                        final_url = resp_url
                    elif check_js and method == 'GET':
                        # Only extract JS/meta redirects from the original short link page
                        body = _decode_body(resp)
                        # Check body for captcha even if URL didn't change
                        if _body_looks_like_captcha(body):
                            resp.close()
                            return (start_url, True)  # blocked
                        js_target = extract_js_redirect(body)
                        if js_target:
                            final_url = js_target
                    resp.close()
                    if final_url != start_url:
                        # Follow HTTP chain further (no more JS extraction)
                        return follow_redirects(final_url, check_js=False)
                    break
                except urllib.error.HTTPError as e:
                    err_url = e.geturl() if hasattr(e, 'geturl') else start_url
                    if _is_captcha_or_login(err_url):
                        return (start_url, True)  # blocked
                    if err_url != start_url:
                        return follow_redirects(err_url, check_js=False)
                except Exception:
                    continue
            return (final_url, False)

        try:
            final_url, blocked = follow_redirects(url)
            if blocked:
                self.send_json({
                    'original': url,
                    'expanded': url,
                    'note': 'Blocked by anti-bot protection (captcha/login) — try opening the link in a browser instead'
                })
            elif final_url != url:
                self.send_json({'original': url, 'expanded': final_url})
            else:
                self.send_json({'original': url, 'expanded': url, 'note': 'No redirect followed'})
        except Exception as e:
            self.send_json({'error': str(e)}, 500)

    def send_json(self, data, status=200):
        body = json.dumps(data, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Content-Length', len(body))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        # Quieter logging
        if '/expand' in str(args[0]):
            print(f'  [expand] {args[0]}')
        else:
            pass  # Suppress static file logs

if __name__ == '__main__':
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    print(f'SuperCopy server: http://{HOST}:{PORT}/')
    print(f'  Expand API: http://{HOST}:{PORT}/expand?url=<short_url>')

    class ThreadingServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
        daemon_threads = True
        allow_reuse_address = True

    server = ThreadingServer((HOST, PORT), SuperCopyHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print('\nServer stopped.')
