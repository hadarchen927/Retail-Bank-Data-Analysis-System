import os
# 1. 必须在导入 sklearn 之前禁用多线程检测，防止 Windows 下的并行库冲突
os.environ["OMP_NUM_THREADS"] = "1"

import sys
import pandas as pd
import warnings
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score

# 忽略所有非致命警告
warnings.filterwarnings("ignore")

def main():
    try:
        # --- 1. 参数解析 ---
        if len(sys.argv) < 2:
            print("ERROR: Missing file path")
            return

        file_path = sys.argv[1]
        # 接收前端/Java 传来的 K 值
        k_value = int(sys.argv[2]) if len(sys.argv) > 2 else 3

        if not os.path.exists(file_path):
            print(f"ERROR: File not found at {file_path}")
            return

        # --- 2. 数据读取 (CSV) ---
        df = pd.read_csv(file_path)

        # --- 3. 数据预处理 (对应任务书模块 3) ---
        # 剔除空值及异常年龄数据
        clean_df = df.dropna(subset=['annualIncome', 'totalAssets'])
        clean_df = clean_df[clean_df['age'] > 0]

        if len(clean_df) < 7: # 确保数据量足够支撑 K=6 的计算
            print("ERROR: Not enough data (min 7 rows required)")
            return

        # --- 4. 特征提取与标准化 ---
        X = clean_df[['annualIncome', 'totalAssets']]
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)

        # --- 5. 执行目标 K 值聚类 (用于展示散点图) ---
        kmeans_target = KMeans(n_clusters=k_value, random_state=42, n_init=10)
        clean_df['clusterTag'] = kmeans_target.fit_predict(X_scaled)

        # 计算当前模型的评估指标
        sse_target = kmeans_target.inertia_
        sil_target = silhouette_score(X_scaled, clean_df['clusterTag'])

        # --- 6. 【核心新增】肘部法则数据计算 ---
        # 循环计算 K 从 2 到 6 的 SSE 值，用于前端折线图展示模型优化过程
        elbow_data = []
        for k in range(2, 7):
            temp_km = KMeans(n_clusters=k, random_state=42, n_init=10)
            temp_km.fit(X_scaled)
            # 存储格式：K值_SSE
            elbow_data.append(f"{k}_{temp_km.inertia_}")

        # 将列表转为字符串: "2_500.1,3_300.5,4_200.2..."
        elbow_str = ",".join(elbow_data)

        # --- 7. 结果写回 CSV ---
        # 使用 utf_8_sig 确保 Excel 打开中文不乱码
        clean_df.to_csv(file_path, index=False, encoding='utf_8_sig')

        # --- 8. 最终输出 (供 Java 解析) ---
        # 格式：SUCCESS:ELBOW:数据串:K:当前K:SCORE:当前SSE:SILHOUETTE:当前轮廓系数
        print(f"SUCCESS:ELBOW:{elbow_str}:K:{k_value}:SCORE:{sse_target}:SILHOUETTE:{sil_target}")

    except Exception as e:
        # 将具体错误打印，Java 的 BufferedReader 会捕获到
        print(f"ERROR: {str(e)}")

if __name__ == "__main__":
    main()