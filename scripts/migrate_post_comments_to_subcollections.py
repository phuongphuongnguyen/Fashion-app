import argparse

import firebase_admin
from firebase_admin import credentials, firestore


def main() -> None:
    parser = argparse.ArgumentParser(description="Move posts.comments arrays to comments subcollections.")
    parser.add_argument("--credentials", required=True, help="Firebase service account JSON path")
    parser.add_argument("--project", default="fashion-app-a5e00", help="Firebase project ID")
    parser.add_argument("--clear-array", action="store_true", help="Delete legacy comments array after migrating")
    args = parser.parse_args()

    cred = credentials.Certificate(args.credentials)
    firebase_admin.initialize_app(cred, {"projectId": args.project})
    db = firestore.client()

    scanned = 0
    migrated_posts = 0
    migrated_comments = 0

    for post in db.collection("posts").stream():
        scanned += 1
        data = post.to_dict() or {}
        comments = data.get("comments") or []
        if not comments:
            continue

        batch = db.batch()
        count = 0
        for comment in comments:
            if not isinstance(comment, dict):
                continue
            text = (comment.get("text") or "").strip()
            if not text:
                continue
            comment_id = comment.get("id") or db.collection("posts").document().id
            ref = post.reference.collection("comments").document(comment_id)
            batch.set(
                ref,
                {
                    "id": comment_id,
                    "userId": comment.get("userId") or "",
                    "username": comment.get("username") or "",
                    "avatarRef": comment.get("avatarRef") or comment.get("avatarUrl") or "",
                    "text": text,
                    "createdAt": comment.get("createdAt") or firestore.SERVER_TIMESTAMP,
                },
                merge=True,
            )
            count += 1

        if count == 0:
            continue

        updates = {"commentCount": count}
        if args.clear_array:
            updates["comments"] = firestore.DELETE_FIELD
        batch.update(post.reference, updates)
        batch.commit()
        migrated_posts += 1
        migrated_comments += count

    print(f"Scanned posts: {scanned}")
    print(f"Migrated posts: {migrated_posts}")
    print(f"Migrated comments: {migrated_comments}")


if __name__ == "__main__":
    main()
