import argparse
from collections import defaultdict

import firebase_admin
from firebase_admin import credentials, firestore


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Sync shops revenue/soldCount/productCount from products."
    )
    parser.add_argument("--credentials", required=True, help="Firebase service account JSON path")
    parser.add_argument("--project", default="fashion-app-a5e00", help="Firebase project ID")
    args = parser.parse_args()

    cred = credentials.Certificate(args.credentials)
    firebase_admin.initialize_app(cred, {"projectId": args.project})
    db = firestore.client()

    stats: dict[str, dict[str, float | int]] = defaultdict(
        lambda: {"revenue": 0.0, "soldCount": 0, "productCount": 0}
    )

    scanned_products = 0
    for doc in db.collection("products").stream():
        scanned_products += 1
        data = doc.to_dict() or {}
        shop_id = data.get("shopId") or ""
        if not shop_id:
            continue

        price = float(data.get("price") or 0)
        sold_count = int(data.get("soldCount") or 0)
        revenue = data.get("revenue")
        if revenue is None:
            revenue = price * sold_count

        stats[shop_id]["revenue"] += float(revenue or 0)
        stats[shop_id]["soldCount"] += sold_count
        stats[shop_id]["productCount"] += 1

    batch = db.batch()
    updated_shops = 0
    for shop_id, values in stats.items():
        batch.set(
            db.collection("shops").document(shop_id),
            {
                "revenue": values["revenue"],
                "soldCount": values["soldCount"],
                "productCount": values["productCount"],
                "updatedAt": firestore.SERVER_TIMESTAMP,
            },
            merge=True,
        )
        updated_shops += 1

    if updated_shops:
        batch.commit()

    print(f"Scanned products: {scanned_products}")
    print(f"Updated shops: {updated_shops}")


if __name__ == "__main__":
    main()
