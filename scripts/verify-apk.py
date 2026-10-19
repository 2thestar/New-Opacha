"""Verify the campaign assets, 64-bit native alignment and ZIP download contents.

Use Android SDK apksigner and zipalign separately for signing and ZIP-entry alignment.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import struct
import zipfile


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("apk",type=Path)
    parser.add_argument("--install-zip",type=Path)
    args=parser.parse_args()
    blob=args.apk.read_bytes()
    native=[]
    with zipfile.ZipFile(args.apk) as archive:
        levels=json.loads(archive.read("assets/campaign/levels.json"))["levels"]
        assert len(levels)==48 and set(Counter(level['mode'] for level in levels).values())=={12}, "Campaign coverage is wrong"
        for name in archive.namelist():
            if not name.startswith("lib/") or not name.endswith(".so"): continue
            data=archive.read(name)
            assert data[:6]==b"\x7fELF\x02\x01", f"Expected little-endian ELF64: {name}"
            offset=struct.unpack_from("<Q",data,32)[0]
            stride,count=struct.unpack_from("<HH",data,54)
            alignments=[]
            for index in range(count):
                header=offset+index*stride
                if struct.unpack_from("<I",data,header)[0]!=1: continue
                file_offset,vaddr=struct.unpack_from("<QQ",data,header+8)
                alignment=struct.unpack_from("<Q",data,header+48)[0]
                assert alignment>=16384 and file_offset%16384==vaddr%16384, f"PT_LOAD alignment below 16 KiB: {name}"
                alignments.append(alignment)
            assert alignments, f"No loadable segments: {name}"
            native.append(name)
        assert {name.split('/')[1] for name in native}=={"arm64-v8a","x86_64"}, "Missing supported 64-bit ABI"
    if args.install_zip:
        with zipfile.ZipFile(args.install_zip) as archive:
            assert archive.namelist()==[args.apk.name], "Unexpected install ZIP contents"
            assert archive.read(args.apk.name)==blob, "Install ZIP differs from tested APK"
    print(json.dumps(dict(apk=args.apk.name,sha256=hashlib.sha256(blob).hexdigest(),maps=len(levels),native=native,install_zip_verified=bool(args.install_zip))))


if __name__=="__main__":
    main()
