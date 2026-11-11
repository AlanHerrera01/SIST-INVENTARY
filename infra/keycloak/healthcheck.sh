#!/bin/bash
set -euo pipefail
{ printf 'HEAD /health/ready HTTP/1.0\r\n\r\n' >&0; grep -q 'HTTP/1.[01] 200'; } 0<>/dev/tcp/localhost/9000
