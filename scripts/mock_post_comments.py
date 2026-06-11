import argparse
from datetime import datetime, timedelta, timezone
import random
import uuid

import firebase_admin
from firebase_admin import credentials, firestore


COMMENT_POOL = [
    ("Mai Anh", "Set này phối màu đẹp quá."),
    ("Linh Nguyen", "Mẫu này còn size M không shop?"),
    ("Minh Khang", "Ảnh lên dáng ổn, nhìn rất dễ mặc."),
    ("Thao Pham", "Mình thích kiểu này, lưu lại để mua sau."),
    ("Hoang Tran", "Chất vải nhìn có vẻ mát."),
    ("Ngoc Bich", "Shop tư vấn thêm màu khác được không?"),
    ("Gia Huy", "Phối với sneaker trắng chắc hợp."),
    ("Lan Chi", "Đơn giản mà sang."),
    ("Quynh Nhu", "Có freeship cho mẫu này không ạ?"),
    ("Duc Anh", "Form này hợp đi chơi cuối tuần."),
]


def build_comments(count: int) -> list[dict]:
    selected = random.sample(COMMENT_POOL, k=min(count, len(COMMENT_POOL)))
    return [
        {
            "id": str(uuid.uuid4()),
            "username": username,
            "avatarUrl": "",
            "text": text,
            "createdAt": datetime.now(timezone.utc) - timedelta(minutes=index * 17),
        }
        for index, (username, text) in enumerate(selected)
    ]


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed mock comments into Firestore posts.")
    parser.add_argument("--credentials", required=True, help="Firebase service account JSON path")
    parser.add_argument("--project", default="fashion-app-a5e00", help="Firebase project ID")
    parser.add_argument("--min", type=int, default=2, help="Minimum comments per post")
    parser.add_argument("--max", type=int, default=5, help="Maximum comments per post")
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Replace existing comments instead of only filling empty posts",
    )
    args = parser.parse_args()

    if args.min < 0 or args.max < args.min:
        raise SystemExit("--min/--max values are invalid")

    cred = credentials.Certificate(args.credentials)
    firebase_admin.initialize_app(cred, {"projectId": args.project})
    db = firestore.client()

    scanned = 0
    updated = 0
    batch = db.batch()
    pending = 0

    for doc in db.collection("posts").stream():
        scanned += 1
        data = doc.to_dict() or {}
        existing = data.get("comments")
        if existing and not args.overwrite:
            continue

        count = random.randint(args.min, args.max)
        comments = build_comments(count)
        batch.update(
            doc.reference,
            {
                "comments": comments,
                "commentCount": len(comments),
            },
        )
        updated += 1
        pending += 1

        if pending >= 450:
            batch.commit()
            batch = db.batch()
            pending = 0

    if pending:
        batch.commit()

    print(f"Scanned posts: {scanned}")
    print(f"Updated posts: {updated}")


if __name__ == "__main__":
    main()
