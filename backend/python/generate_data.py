import pandas as pd
from faker import Faker
import random
import os
from datetime import datetime, timedelta

fake = Faker('zh_CN')
DATA_DIR = "D:/retail-bank-analysis-system/data/"

def ensure_dir_exists():
    if not os.path.exists(DATA_DIR): os.makedirs(DATA_DIR)

def generate_customer_data(count=1000):
    customers = []
    print(f"正在生成 {count} 条客户数据(CSV)...")
    for i in range(1, count + 1):
        income = round(random.uniform(20000, 1000000), 2)
        assets = round(income * random.uniform(0.5, 15), 2)
        # 故意制造少量脏数据（5%概率）
        dice = random.random()
        record = {
            "custName": fake.name() if dice > 0.05 else "", # 可能缺失姓名
            "gender": random.choice(["男", "女"]),
            "age": random.randint(18, 80) if dice > 0.05 else -1, # 可能年龄异常
            "annualIncome": income,
            "totalAssets": assets,
            "loanBalance": round(random.uniform(0, 500000), 2), # 贷款余额
            "creditScore": random.randint(300, 850), # 信用评分
            "overdueDays": random.randint(0, 30) if random.random() > 0.8 else 0 # 20%概率逾期
        }
        customers.append(record)

    df = pd.DataFrame(customers)
    # 使用 utf_8_sig 确保 Excel 打开不乱码
    df.to_csv(os.path.join(DATA_DIR, "bank_customers.csv"), index=False, encoding='utf_8_sig')

def generate_transaction_data(customer_count=1000):
    transactions = []
    types = ['存入', '支出', '转账']
    print(f"正在生成流水数据(CSV)...")
    for _ in range(customer_count * 10):
        record = {
            "custId": random.randint(1, customer_count),
            "amount": round(random.uniform(1, 20000), 2),
            "type": random.choice(types),
            "transTime": (datetime.now() - timedelta(days=random.randint(0, 90))).strftime("%Y-%m-%d %H:%M:%S"),
            "location": fake.city()
        }
        transactions.append(record)
    df = pd.DataFrame(transactions)
    df.to_csv(os.path.join(DATA_DIR, "bank_transactions.csv"), index=False, encoding='utf_8_sig')

if __name__ == "__main__":
    ensure_dir_exists()
    generate_customer_data(1000)
    generate_transaction_data(1000)
    print("✅ CSV 数据生成完毕！")
