#!/usr/bin/env python3
"""SuperCopy local server — serves index.html + URL expansion API (no CORS issues)."""
import http.server
import socketserver
import urllib.request
import urllib.parse
import urllib.error
import json
import sys
import os

PORT = 8080
HOST = '0.0.0.0'

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
            req.add_header('User-Agent',
                'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
                'AppleWebKit/537.36 (KHTML, like Gecko) '
                'Chrome/130.0.0.0 Safari/537.36'
            )
            return urllib.request.urlopen(req, timeout=10)

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

        def follow_redirects(start_url, check_js=True):
            """Follow HTTP redirect chain and return final URL.
            JS/meta redirect extraction only runs on check_js=True (first level only)."""
            final_url = start_url
            for method in ['GET', 'HEAD']:
                try:
                    resp = make_request(start_url, method)
                    resp_url = resp.geturl()
                    if resp_url != start_url:
                        # HTTP redirect followed — recurse without JS extraction
                        final_url = resp_url
                    elif check_js and method == 'GET':
                        # Only extract JS/meta redirects from the original short link page
                        charset = resp.headers.get_content_charset() or 'utf-8'
                        body = resp.read().decode(charset, errors='ignore')
                        js_target = extract_js_redirect(body)
                        if js_target:
                            final_url = js_target
                    resp.close()
                    if final_url != start_url:
                        # Follow HTTP chain further (no more JS extraction)
                        return follow_redirects(final_url, check_js=False)
                    break
                except urllib.error.HTTPError as e:
                    final_url = e.geturl() if hasattr(e, 'geturl') else start_url
                    if final_url != start_url:
                        return follow_redirects(final_url, check_js=False)
                except Exception:
                    continue
            return final_url

        try:
            final_url = follow_redirects(url)
            if final_url != url:
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
