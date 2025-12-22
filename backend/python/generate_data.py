import pandas as pd
from faker import Faker
import random
import os
from datetime import datetime, timedelta

# 初始化中文 Faker
fake = Faker('zh_CN')

# 配置文件保存路径
DATA_DIR = "D:/retail-bank-analysis-system/data/"

def ensure_dir_exists():
    """确保目录存在"""
    if not os.path.exists(DATA_DIR):
        os.makedirs(DATA_DIR)
        print(f"创建目录: {DATA_DIR}")

def generate_customer_data(count=100):
    """生成客户基础数据 (用于导入 bank_customer 表)"""
    customers = []
    print(f"正在生成 {count} 条客户数据...")

    for i in range(1, count + 1):
        income = round(random.uniform(30000, 800000), 2)
        # 资产逻辑：通常与收入正相关，加上随机波动
        assets = round(income * random.uniform(0.8, 12), 2)
        # 贷款逻辑：50% 概率有贷款
        loan = round(random.uniform(1000, 300000), 2) if random.random() > 0.5 else 0

        record = {
            "custName": fake.name(),
            "gender": random.choice(["男", "女"]),
            "age": random.randint(18, 75),
            "annualIncome": income,
            "totalAssets": assets,
            "loanBalance": loan
        }
        customers.append(record)

    df = pd.DataFrame(customers)
    file_path = os.path.join(DATA_DIR, "bank_customers.xlsx")
    df.to_excel(file_path, index=False)
    print(f"✅ 客户数据已生成: {file_path}")
    return count

def generate_transaction_data(customer_count=100):
    """生成交易流水数据 (用于导入 bank_transaction 表)"""
    transactions = []
    types = ['存入', '支出', '转账']
    # 假设数据库导入后，ID是从1开始的自增序列
    customer_ids = range(1, customer_count + 1)

    print(f"正在为 {customer_count} 个客户生成交易流水...")

    for c_id in customer_ids:
        # 每个客户随机生成 5 到 15 条流水
        for _ in range(random.randint(5, 15)):
            # 模拟最近 60 天内的交易
            random_days = random.randint(0, 60)
            trans_time = datetime.now() - timedelta(days=random_days, hours=random.randint(0,23))

            record = {
                "custId": c_id,
                "amount": round(random.uniform(10, 8000), 2),
                "type": random.choice(types),
                "transTime": trans_time.strftime("%Y-%m-%d %H:%M:%S"),
                "location": random.choice(['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉'])
            }
            transactions.append(record)

    df = pd.DataFrame(transactions)
    file_path = os.path.join(DATA_DIR, "bank_transactions.xlsx")
    df.to_excel(file_path, index=False)
    print(f"✅ 交易流水已生成: {file_path}")

if __name__ == "__main__":
    try:
        # 1. 确保目录存在
        ensure_dir_exists()

        # 2. 生成 100 名客户（你可以把这个数字改大，比如 1000）
        count = 100
        generate_customer_data(count)

        # 3. 生成对应的交易流水
        generate_transaction_data(count)

        print("\n" + "="*30)
        print("所有模拟数据生成完毕！")
        print(f"请前往 {DATA_DIR} 查看生成的文件。")
        print("="*30)

    except Exception as e:
        print(f"❌ 生成失败: {str(e)}")