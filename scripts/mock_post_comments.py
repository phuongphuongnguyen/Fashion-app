import argparse
from datetime import datetime, timedelta, timezone
import random
import uuid

import firebase_admin
from firebase_admin import credentials, firestore


COMMENT_TEXTS = [
    "Set nay phoi mau dep qua.",
    "Mau nay con size M khong shop?",
    "Anh len dang on, nhin rat de mac.",
    "Minh thich kieu nay, luu lai de mua sau.",
    "Chat vai nhin co ve mat.",
    "Shop tu van them mau khac duoc khong?",
    "Phoi voi sneaker trang chac hop.",
    "Don gian ma sang.",
    "Co freeship cho mau nay khong a?",
    "Form nay hop di choi cuoi tuan.",
]


def load_users(db) -> list[dict]:
    users = []
    for doc in db.collection("users").stream():
        data = doc.to_dict() or {}
        name = (
            or data.get("username")
            or data.get("name")
            or data.get("displayName")
            or data.get("email")
            or ""
        )
        if not name:
            continue
        users.append(
            {
                "id": doc.id,
                "username": name,
                "avatarRef": data.get("avatarRef") or "",
            }
        )
    return users


def build_comments(users: list[dict], count: int) -> list[dict]:
    selected_users = random.sample(users, k=min(count, len(users)))
    selected_texts = random.sample(COMMENT_TEXTS, k=min(count, len(COMMENT_TEXTS)))
    return [
        {
            "id": str(uuid.uuid4()),
            "userId": user["id"],
            "username": user["username"],
            "avatarRef": user["avatarRef"],
            "text": text,
            "createdAt": datetime.now(timezone.utc) - timedelta(minutes=index * 17),
        }
        for index, (user, text) in enumerate(zip(selected_users, selected_texts))
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
    users = load_users(db)

    if not users:
        raise SystemExit("No usable users found in Firestore users collection")

    scanned = 0
    updated = 0
    batch = db.batch()
    pending = 0

    for doc in db.collection("posts").stream():
        scanned += 1
        data = doc.to_dict() or {}
        existing = data.get("comments") or list(
            doc.reference.collection("comments").limit(1).stream()
        )
        if existing and not args.overwrite:
            continue

        if args.overwrite:
            for old_comment in doc.reference.collection("comments").stream():
                batch.delete(old_comment.reference)
                pending += 1
                if pending >= 450:
                    batch.commit()
                    batch = db.batch()
                    pending = 0

        count = random.randint(args.min, args.max)
        comments = build_comments(users, count)
        for comment in comments:
            comment_ref = doc.reference.collection("comments").document(comment["id"])
            batch.set(comment_ref, comment)
            pending += 1

            if pending >= 450:
                batch.commit()
                batch = db.batch()
                pending = 0

        updates = {"commentCount": len(comments)}
        if args.overwrite:
            updates["comments"] = firestore.DELETE_FIELD
        batch.update(doc.reference, updates)
        updated += 1
        pending += 1

        if pending >= 450:
            batch.commit()
            batch = db.batch()
            pending = 0

    if pending:
        batch.commit()

    print(f"Loaded users: {len(users)}")
    print(f"Scanned posts: {scanned}")
    print(f"Updated posts: {updated}")


if __name__ == "__main__":
    main()
