import os
# 1. 必须在导入 sklearn 之前禁用多线程检测，防止 Windows 下的 split 报错
os.environ["OMP_NUM_THREADS"] = "1"

import sys
import pandas as pd
import warnings
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler # 【新增】标准化工具
from sklearn.metrics import silhouette_score     # 【新增】轮廓系数评价指标

# 忽略所有非致命警告
warnings.filterwarnings("ignore")

def main():
    try:
        # 1. 检查输入参数
        if len(sys.argv) < 2:
            print("ERROR: No file path provided")
            return

        file_path = sys.argv[1]

        if not os.path.exists(file_path):
            print(f"ERROR: File not found at {file_path}")
            return

        # 2. 读取 Excel 数据 (使用 openpyxl 引擎)
        df = pd.read_excel(file_path, engine='openpyxl')

        # 3. 数据清洗与预处理
        # 剔除包含空值的行，确保计算不崩溃
        clean_df = df.dropna(subset=['annualIncome', 'totalAssets'])

        if len(clean_df) < 3:
            print("ERROR: Not enough valid data (need at least 3 rows to form 3 clusters)")
            return

        # 4. 特征选择
        X = clean_df[['annualIncome', 'totalAssets']]

        # 5. 【核心优化】特征标准化 (Normalization)
        # 目的：消除收入和资产之间量纲（单位）的影响，让两个指标同等重要
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)

        # 6. 执行 K-Means 聚类
        # n_clusters=3 (划分为高、中、低三类群体)
        # random_state=42 保证每次运行结果一致，方便写论文截图
        kmeans = KMeans(n_clusters=3, random_state=42, n_init=10)

        # 将聚类结果写回原 DataFrame 的标签列
        clean_df['clusterTag'] = kmeans.fit_predict(X_scaled)

        # 7. 模型多维度评估
        # SSE (Inertia): 簇内误差平方和，越小代表簇内越紧凑
        sse_score = kmeans.inertia_

        # Silhouette Score (轮廓系数): 衡量聚类的凝聚力和分离度
        # 取值范围 [-1, 1]，越接近 1 说明分类越完美
        sil_score = silhouette_score(X_scaled, clean_df['clusterTag'])

        # 8. 将计算后的数据写回原 Excel 文件
        clean_df.to_excel(file_path, index=False)

        # 9. 【关键修改】打印成功标志和多维度分数，供 Java 解析
        # 我们把 SSE 和 轮廓系数都传回去，让系统更显专业
        print(f"SUCCESS:SCORE:{sse_score}:SILHOUETTE:{sil_score}")

    except Exception as e:
        # 捕获所有异常并打印，方便在 Java 控制台查看
        print(f"ERROR: {str(e)}")

if __name__ == "__main__":
    main()