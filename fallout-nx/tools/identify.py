#!/usr/bin/env python3
"""Identifies a game executable (default.xex) from your own copy: its SHA-256 and the XEX2 header.

Static recompilation is tied to one exact executable: function addresses, jump tables and every override in
games/<game>/overrides.toml only hold for the build they were found in. This tool says which build you have, and
checks it against the builds listed in games/<game>/versions.toml.

Usage:
    python3 tools/identify.py <game> <default.xex>            print the identity, check it against versions.toml
    python3 tools/identify.py <game> <default.xex> --write F  also write it to F (key = value lines)

Only the plain XEX2 header is read (it is not encrypted). Nothing is decrypted or decompressed here: the code
generator does that. Field layouts follow sdk/include/rex/system/util/xex2_info.h of the SDK.
"""
import argparse
import hashlib
import os
import struct
import sys
import tomllib

XEX_HEADER_ENTRY_POINT = 0x00010100
XEX_HEADER_IMAGE_BASE_ADDRESS = 0x00010201
XEX_HEADER_EXECUTION_INFO = 0x00040006

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def version_str(value):
    # xex2_version: major:4 minor:4 build:16 qfe:8, from the most significant bit down.
    return f'{value >> 28}.{(value >> 24) & 0xF}.{(value >> 8) & 0xFFFF}.{value & 0xFF}'


def read_header(path):
    with open(path, 'rb') as f:
        head = f.read(0x18)
        if len(head) < 0x18 or head[:4] != b'XEX2':
            raise ValueError(f'{path}: not an XEX2 executable')
        _, flags, header_size, _, _, count = struct.unpack('>4sIIIII', head)
        f.seek(0)
        blob = f.read(header_size)
    info = {'module_flags': f'0x{flags:08X}'}
    for i in range(count):
        key, value = struct.unpack_from('>II', blob, 0x18 + i * 8)
        if key == XEX_HEADER_ENTRY_POINT:
            info['entry_point'] = f'0x{value:08X}'
        elif key == XEX_HEADER_IMAGE_BASE_ADDRESS:
            info['image_base'] = f'0x{value:08X}'
        elif key == XEX_HEADER_EXECUTION_INFO:
            media_id, ver, base_ver, title_id, platform, exe_type, disc, discs, _ = \
                struct.unpack_from('>IIIIBBBBI', blob, value)
            info.update(title_id=f'{title_id:08X}', media_id=f'{media_id:08X}', version=version_str(ver),
                        base_version=version_str(base_ver), disc=f'{disc}/{discs}')
    return info


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('game', help='game folder under games/ (fo3, fnv)')
    ap.add_argument('xex', help='path to your default.xex')
    ap.add_argument('--write', help='write the identity to this file')
    args = ap.parse_args()

    with open(os.path.join(ROOT, 'games', args.game, 'versions.toml'), 'rb') as f:
        known = tomllib.load(f)

    try:
        info = read_header(args.xex)
    except (OSError, ValueError, struct.error) as e:
        print(f'error: {e}', file=sys.stderr)
        return 1
    info['sha256'] = sha256(args.xex)
    info['size'] = str(os.path.getsize(args.xex))
    for k, v in info.items():
        print(f'{k:13} {v}')

    expected_title = known.get('title_id', '')
    if expected_title and info.get('title_id') != expected_title.upper():
        print(f'\nWARNING: title id {info.get("title_id")} is not {args.game} ({expected_title}). Wrong disc or game?')

    match = next((b for b in known.get('build', []) if b.get('sha256', '').lower() == info['sha256']), None)
    if match:
        print(f'\nKnown build: {match.get("name", "?")}')
        info['build'] = match.get('name', '')
    else:
        print(f'\nUnknown build. If this is your dump, add it to games/{args.game}/versions.toml as a [[build]] entry:')
        print(f'  [[build]]\n  name = "<edition / region / title update>"\n  sha256 = "{info["sha256"]}"\n'
              f'  version = "{info.get("version", "")}"\n  media_id = "{info.get("media_id", "")}"')

    if args.write:
        os.makedirs(os.path.dirname(os.path.abspath(args.write)), exist_ok=True)
        with open(args.write, 'w') as f:
            f.writelines(f'{k} = {v}\n' for k, v in info.items())
    return 0


if __name__ == '__main__':
    sys.exit(main())
