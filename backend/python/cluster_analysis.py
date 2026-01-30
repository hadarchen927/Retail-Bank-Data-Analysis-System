# backend/python/cluster_analysis.py
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
        if len(sys.argv) < 2:
            print("ERROR: Missing file path")
            return

        file_path = sys.argv[1]
        k_value = int(sys.argv[2]) if len(sys.argv) > 2 else 3

        if not os.path.exists(file_path):
            print(f"ERROR: File not found at {file_path}")
            return

        # 【支持两种格式】自动识别文件类型
        if file_path.endswith('.csv'):
            df = pd.read_csv(file_path)
            file_type = 'csv'
        elif file_path.endswith('.xlsx'):
            df = pd.read_excel(file_path, engine='openpyxl')
            file_type = 'xlsx'
        else:
            print("ERROR: Unsupported file format. Use .csv or .xlsx")
            return

        # 数据预处理
        clean_df = df.dropna(subset=['annualIncome', 'totalAssets'])
        clean_df = clean_df[clean_df['age'] > 0]

        if len(clean_df) < 7:
            print("ERROR: Not enough data (min 7 rows required)")
            return

        # 特征提取
        X = clean_df[['annualIncome', 'totalAssets']]
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)

        # 执行聚类
        kmeans_target = KMeans(n_clusters=k_value, random_state=42, n_init=10)
        clean_df['clusterTag'] = kmeans_target.fit_predict(X_scaled)

        sse_target = kmeans_target.inertia_
        sil_target = silhouette_score(X_scaled, clean_df['clusterTag'])

        # 肘部法则
        elbow_data = []
        for k in range(2, 7):
            temp_km = KMeans(n_clusters=k, random_state=42, n_init=10)
            temp_km.fit(X_scaled)
            elbow_data.append(f"{k}_{temp_km.inertia_}")

        elbow_str = ",".join(elbow_data)

        # 【写回原格式】
        if file_type == 'csv':
            clean_df.to_csv(file_path, index=False, encoding='utf_8_sig')
        else:
            clean_df.to_excel(file_path, index=False, engine='openpyxl')

        # 输出结果
        print(f"SUCCESS:ELBOW:{elbow_str}:K:{k_value}:SCORE:{sse_target}:SILHOUETTE:{sil_target}")

    except Exception as e:
        print(f"ERROR: {str(e)}")

if __name__ == "__main__":
    main()