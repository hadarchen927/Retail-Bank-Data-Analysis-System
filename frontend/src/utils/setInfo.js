export function setPageInfo(title) {
    // 1. 修改标题
    document.title = title + " - 零售银行数据分析系统";

    // 2. 找到或创建图标 link 标签
    let link = document.querySelector("link[rel*='icon']");
    if (!link) {
        link = document.createElement('link');
        link.rel = 'shortcut icon';
        link.type = 'image/png';
        document.head.appendChild(link);
    }

    // 【核心修正】
    link.href = '/icon.png?v=' + new Date().getTime();

    console.log("正在尝试设置图标路径为: ", link.href);
}