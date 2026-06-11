import argparse

import firebase_admin
from firebase_admin import credentials, firestore


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Set products/{id}.revenue = soldCount * price for mock data."
    )
    parser.add_argument("--credentials", required=True, help="Firebase service account JSON path")
    parser.add_argument("--project", default="fashion-app-a5e00", help="Firebase project ID")
    args = parser.parse_args()

    cred = credentials.Certificate(args.credentials)
    firebase_admin.initialize_app(cred, {"projectId": args.project})
    db = firestore.client()

    scanned = 0
    updated = 0
    batch = db.batch()
    pending = 0

    for doc in db.collection("products").stream():
        scanned += 1
        data = doc.to_dict() or {}
        price = data.get("price") or 0
        sold_count = data.get("soldCount") or 0

        try:
            revenue = float(price) * int(sold_count)
        except (TypeError, ValueError):
            revenue = 0.0

        batch.update(doc.reference, {"revenue": revenue})
        updated += 1
        pending += 1

        if pending >= 450:
            batch.commit()
            batch = db.batch()
            pending = 0

    if pending:
        batch.commit()

    print(f"Scanned products: {scanned}")
    print(f"Updated products: {updated}")


if __name__ == "__main__":
    main()
