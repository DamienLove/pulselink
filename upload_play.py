import os
import pathlib
import time
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from googleapiclient.errors import HttpError

REPO_ROOT = pathlib.Path(__file__).resolve().parent

def resolve_service_account_key() -> pathlib.Path:
    env_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    candidates = [
        pathlib.Path(env_path) if env_path else None,
        REPO_ROOT / "secrets" / "service-account-key.json",
        pathlib.Path("C:/Projects/pulselink/secrets/service-account-key.json"),
    ]
    for candidate in candidates:
        if candidate is not None and candidate.exists():
            return candidate
    raise SystemExit("service account key missing")

svc_path = resolve_service_account_key()
creds = service_account.Credentials.from_service_account_file(
    svc_path,
    scopes=['https://www.googleapis.com/auth/androidpublisher']
)
service = build('androidpublisher', 'v3', credentials=creds)
DEFAULT_RETRIES = 5

def upload_and_release(package, aab_path, version_code, name):
    print(f"-- {package} uploading {aab_path}")
    edit = service.edits().insert(body={}, packageName=package).execute()
    edit_id = edit['id']
    media = MediaFileUpload(
        aab_path,
        mimetype='application/octet-stream',
        resumable=True,
        chunksize=4 * 1024 * 1024,  # 4MB chunks to reduce timeout risk
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
        except HttpError as exc:
            message = str(exc)
            if "already been used" in message:
                print("  version already uploaded; skipping")
                return
            raise
    uploaded_vc = int(bundle['versionCode'])
    if uploaded_vc != version_code:
        print(f"warning: uploaded versionCode {uploaded_vc} != expected {version_code}")
    release = {
        'name': name,
        'versionCodes': [str(uploaded_vc)],
        'status': 'completed'
    }
    for track in ['internal', 'beta', 'production']:
        service.edits().tracks().update(
            packageName=package,
            editId=edit_id,
            track=track,
            body={'releases': [release]}
        ).execute(num_retries=DEFAULT_RETRIES)
        print(f"  set track {track}")
    for attempt in range(DEFAULT_RETRIES):
        try:
            service.edits().commit(packageName=package, editId=edit_id).execute(num_retries=DEFAULT_RETRIES)
            print(f"  committed edit {edit_id}")
            return
        except HttpError as exc:
            message = str(exc)
            if "not completed yet" in message and attempt < DEFAULT_RETRIES - 1:
                wait = (attempt + 1) * 5
                print(f"  upload still processing, retrying commit in {wait}s...")
                time.sleep(wait)
                continue
            raise

base = REPO_ROOT / 'disposable_aabs'
upload_and_release('com.free.pulselink', str(base/'androidApp-free-release-v143.aab'), 143, 'v143')
upload_and_release('com.pulselink.pro', str(base/'androidApp-pro-release-v143.aab'), 143, 'v143')

