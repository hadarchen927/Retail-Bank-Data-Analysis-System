import os
os.environ["OMP_NUM_THREADS"] = "1"
import sys
import pandas as pd
import warnings
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score

warnings.filterwarnings("ignore")

def main():
    try:
        file_path = sys.argv[1]
        k_value = int(sys.argv[2]) if len(sys.argv) > 2 else 3

        # 1. 读取 CSV
        df = pd.read_csv(file_path)

        # 2. 数据清洗
        clean_df = df.dropna(subset=['annualIncome', 'totalAssets'])
        clean_df = clean_df[clean_df['age'] > 0] # 剔除之前生成的脏数据

        # 3. 特征标准化与聚类
        X = clean_df[['annualIncome', 'totalAssets']]
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)

        kmeans = KMeans(n_clusters=k_value, random_state=42, n_init=10)
        clean_df['clusterTag'] = kmeans.fit_predict(X_scaled)

        # 4. 指标计算
        sse = kmeans.inertia_
        sil = silhouette_score(X_scaled, clean_df['clusterTag'])

        # 5. 写回 CSV (关键：带 BOM 的 UTF-8)
        clean_df.to_csv(file_path, index=False, encoding='utf_8_sig')

        print(f"SUCCESS:K:{k_value}:SCORE:{sse}:SILHOUETTE:{sil}")

    except Exception as e:
        print(f"ERROR: {str(e)}")

if __name__ == "__main__":
    main()