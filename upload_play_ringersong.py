import pathlib

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

svc_path = pathlib.Path("C:/Projects/pulselink/secrets/service-account-key.json")
if not svc_path.exists():
    raise SystemExit("service account key missing")

creds = service_account.Credentials.from_service_account_file(
    svc_path,
    scopes=["https://www.googleapis.com/auth/androidpublisher"],
)
service = build("androidpublisher", "v3", credentials=creds)

DEFAULT_RETRIES = 5


def upload_and_release(package, aab_path, version_code, name, tracks):
    print(f"-- {package} uploading {aab_path}")
    edit = service.edits().insert(body={}, packageName=package).execute()
    edit_id = edit["id"]
    media = MediaFileUpload(
        aab_path,
        mimetype="application/octet-stream",
        resumable=True,
        chunksize=4 * 1024 * 1024,
    )
    upload = service.edits().bundles().upload(
        packageName=package,
        editId=edit_id,
        media_body=media,
    )
    bundle = None
    while bundle is None:
        try:
            status, bundle = upload.next_chunk(num_retries=DEFAULT_RETRIES)
            if status:
                pct = int(status.progress() * 100)
                print(f"  upload progress {pct}%")
        except TimeoutError:
            print("  timeout during upload chunk, retrying...")
            continue
    uploaded_vc = int(bundle["versionCode"])
    if uploaded_vc != version_code:
        print(f"warning: uploaded versionCode {uploaded_vc} != expected {version_code}")
    release = {
        "name": name,
        "versionCodes": [str(uploaded_vc)],
        "status": "completed",
    }
    for track in tracks:
        service.edits().tracks().update(
            packageName=package,
            editId=edit_id,
            track=track,
            body={"releases": [release]},
        ).execute(num_retries=DEFAULT_RETRIES)
        print(f"  set track {track}")
    service.edits().commit(packageName=package, editId=edit_id).execute(
        num_retries=DEFAULT_RETRIES
    )
    print(f"  committed edit {edit_id}")


base = pathlib.Path("C:/Projects/pulselink/disposable_aabs")
upload_and_release(
    "com.RingerSong.free",
    str(base / "ringersong-release-v27.aab"),
    27,
    "v27",
    ["internal", "alpha", "beta"],
)
