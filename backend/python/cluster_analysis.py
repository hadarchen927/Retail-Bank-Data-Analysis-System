import os
# 1. 必须在导入核心库之前禁用多线程检测，防止 Windows 环境下的底层 split 报错
os.environ["OMP_NUM_THREADS"] = "1"

import sys
import pandas as pd
import warnings
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score

# 忽略所有非致命警告（如版本更新警告），保证 Java 拿到的输出纯净
warnings.filterwarnings("ignore")

def main():
    try:
        # --- 1. 参数解析 ---
        if len(sys.argv) < 2:
            print("ERROR: Missing file path argument")
            return

        file_path = sys.argv[1]

        # 接收来自 Java 的 K 值参数，如果没有传则默认为 3
        # 对应任务书模块 8：模型的训练与参数调整
        k_value = int(sys.argv[2]) if len(sys.argv) > 2 else 3

        if not os.path.exists(file_path):
            print(f"ERROR: File not found at {file_path}")
            return

        # --- 2. 数据读取 (使用 openpyxl 引擎) ---
        df = pd.read_excel(file_path, engine='openpyxl')

        # --- 3. 数据清洗与预处理 ---
        # 剔除年收入或总资产为空的行，确保算法不会崩溃
        # 对应任务书模块 3：数据清洗与预处理
        clean_df = df.dropna(subset=['annualIncome', 'totalAssets'])

        # 检查有效数据量是否满足聚类要求
        if len(clean_df) < k_value:
            print(f"ERROR: Not enough valid data ({len(clean_df)} rows) for {k_value} clusters")
            return

        # --- 4. 特征提取与标准化 ---
        X = clean_df[['annualIncome', 'totalAssets']]

        # 使用标准化工具 (Z-Score)，消除资产(百万级)与收入(十万级)之间的量纲差异
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)

        # --- 5. 执行 K-Means 聚类训练 ---
        # random_state=42 确保实验可重复性（论文截图数据一致）
        kmeans = KMeans(n_clusters=k_value, random_state=42, n_init=10)

        # 执行预测并将标签写回 DataFrame
        clean_df['clusterTag'] = kmeans.fit_predict(X_scaled)

        # --- 6. 模型评估 (用于任务书模块 8 的指标展示) ---
        # SSE (Inertia): 越小说明簇内越紧凑
        sse_score = kmeans.inertia_

        # Silhouette Score: 越接近 1 说明分群越科学
        sil_score = silhouette_score(X_scaled, clean_df['clusterTag'])

        # --- 7. 结果持久化 ---
        # 将带有 clusterTag 的结果保存回 Excel，供 Java 读回数据库
        clean_df.to_excel(file_path, index=False)

        # --- 8. 格式化输出 (供 Java 解析) ---
        # 增加 K 值记录，方便 Java 日志记录
        print(f"SUCCESS:K:{k_value}:SCORE:{sse_score}:SILHOUETTE:{sil_score}")

    except Exception as e:
        # 捕获所有异常并打印，Java 的 ProcessBuilder 会抓取到这些信息
        print(f"ERROR: {str(e)}")

if __name__ == "__main__":
    main()