import argparse

import firebase_admin
from firebase_admin import credentials, firestore


LEGACY_FIELDS = ("revenueOrderCount", "revenueSoldCount")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Delete legacy product revenue counter fields from Firestore."
    )
    parser.add_argument("--credentials", required=True, help="Firebase service account JSON path")
    parser.add_argument("--project", default="fashion-app-a5e00", help="Firebase project ID")
    args = parser.parse_args()

    cred = credentials.Certificate(args.credentials)
    firebase_admin.initialize_app(cred, {"projectId": args.project})
    db = firestore.client()

    products = db.collection("products").stream()
    batch = db.batch()
    pending = 0
    touched = 0
    scanned = 0

    for doc in products:
        scanned += 1
        data = doc.to_dict() or {}
        fields_to_delete = {
            field: firestore.DELETE_FIELD
            for field in LEGACY_FIELDS
            if field in data
        }
        if not fields_to_delete:
            continue

        batch.update(doc.reference, fields_to_delete)
        pending += 1
        touched += 1

        if pending >= 450:
            batch.commit()
            batch = db.batch()
            pending = 0

    if pending:
        batch.commit()

    print(f"Scanned products: {scanned}")
    print(f"Updated products: {touched}")
    print(f"Deleted fields: {', '.join(LEGACY_FIELDS)}")


if __name__ == "__main__":
    main()
