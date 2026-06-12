import argparse
from collections import defaultdict
from datetime import datetime, timezone

import firebase_admin
from firebase_admin import credentials, firestore


def delete_collection(collection_ref, batch_size: int = 200) -> int:
    deleted = 0
    while True:
        docs = list(collection_ref.limit(batch_size).stream())
        if not docs:
            return deleted
        batch = collection_ref._client.batch()
        for doc in docs:
            batch.delete(doc.reference)
            deleted += 1
        batch.commit()


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Rebuild daily revenue documents from current product revenue/soldCount mock data."
    )
    parser.add_argument("--credentials", required=True, help="Firebase service account JSON path")
    parser.add_argument("--project", default="fashion-app-a5e00", help="Firebase project ID")
    parser.add_argument("--day", default=datetime.now(timezone.utc).strftime("%Y-%m-%d"))
    args = parser.parse_args()

    cred = credentials.Certificate(args.credentials)
    firebase_admin.initialize_app(cred, {"projectId": args.project})
    db = firestore.client()

    products = list(db.collection("products").stream())
    shop_stats = defaultdict(lambda: {"revenue": 0.0, "soldCount": 0, "orderCount": 0})
    total_revenue = 0.0
    total_sold = 0
    deleted_daily_docs = 0

    batch = db.batch()
    pending = 0
    for product in products:
        data = product.to_dict() or {}
        revenue = float(data.get("revenue") or 0)
        sold_count = int(data.get("soldCount") or 0)
        order_count = int(data.get("orderCount") or 0)
        if order_count <= 0:
            order_count = sold_count if sold_count > 0 else 0

        deleted_daily_docs += delete_collection(product.reference.collection("dailyRevenue"))
        batch.set(
            product.reference.collection("dailyRevenue").document(args.day),
            {
                "revenue": revenue,
                "soldCount": sold_count,
                "orderCount": order_count,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            },
            merge=True,
        )
        pending += 1

        shop_id = data.get("shopId") or ""
        if shop_id:
            shop_stats[shop_id]["revenue"] += revenue
            shop_stats[shop_id]["soldCount"] += sold_count
            shop_stats[shop_id]["orderCount"] += order_count
        total_revenue += revenue
        total_sold += sold_count

        if pending >= 450:
            batch.commit()
            batch = db.batch()
            pending = 0

    for shop_id, stats in shop_stats.items():
        shop_ref = db.collection("shops").document(shop_id)
        deleted_daily_docs += delete_collection(shop_ref.collection("dailyRevenue"))
        batch.set(
            shop_ref.collection("dailyRevenue").document(args.day),
            {
                "revenue": stats["revenue"],
                "soldCount": stats["soldCount"],
                "orderCount": stats["orderCount"],
                "updatedAt": firestore.SERVER_TIMESTAMP,
            },
            merge=True,
        )
        pending += 1
        if pending >= 450:
            batch.commit()
            batch = db.batch()
            pending = 0

    deleted_analytics = delete_collection(db.collection("analytics_daily"))
    batch.set(
        db.collection("analytics_daily").document(args.day),
        {
            "grossRevenue": total_revenue,
            "netRevenue": total_revenue,
            "orderCount": sum(int(stats["orderCount"]) for stats in shop_stats.values()),
            "soldCount": total_sold,
            "updatedAt": firestore.SERVER_TIMESTAMP,
        },
        merge=True,
    )
    pending += 1

    if pending:
        batch.commit()

    print(f"Scanned products: {len(products)}")
    print(f"Updated product daily docs: {len(products)}")
    print(f"Updated shop daily docs: {len(shop_stats)}")
    print(f"Deleted old daily docs: {deleted_daily_docs}")
    print(f"Deleted analytics docs: {deleted_analytics}")
    print(f"Day: {args.day}")


if __name__ == "__main__":
    main()
