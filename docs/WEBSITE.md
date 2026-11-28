# Errata 官网

`website/` 是 Errata（错题本）的静态官网，零构建、零运行时依赖，直接由 GitHub Pages 托管。

线上地址（部署后）：<https://chenzhizixin2010.github.io/wrongbook/>

## 目录

```
website/
├── index.html            首页
├── privacy.html          隐私政策（正式页）
├── 404.html              GitHub Pages 404
├── site.webmanifest      PWA 图标与主题色
├── robots.txt / sitemap.xml
├── .nojekyll             关闭 Jekyll 处理
└── assets/
    ├── css/site.css
    ├── js/site.js
    └── img/              图标、OG 图、7 张截图（WebP）
```

## 本地预览

在仓库根目录：

```powershell
python -m http.server 8080 --directory website
```

打开 <http://127.0.0.1:8080/>。不要直接双击 `index.html`：相对路径和字体渲染会有差异。

## 部署到 GitHub Pages

1. 提交并推送 `website/` 与 `.github/workflows/pages.yml`：

   ```powershell
   $env:Path = "D:\dev-tools\git\cmd;" + $env:Path
   git add website .github/workflows/pages.yml docs/WEBSITE.md
   git commit -m "site: add official website and GitHub Pages workflow"
   git push origin main
   ```

   > 如果本地 `main` 比 `origin/main` 多出了一些还没推送的提交，上面这条 `push` 会把这些提交一起发布。只想发布官网的话，请从 `origin/main` 拉一个分支、只提交 `website/`、`pages.yml` 和本说明，再开 PR 合并。

2. 打开仓库 **Settings → Pages**，把 **Build and deployment → Source** 设为 **GitHub Actions**。
   （工作流里带了 `enablement: true`，部分仓库可以自动开启；如果首次运行报 Pages 未启用，按这一步手动设置后重跑。）
3. 打开 **Actions → Deploy website to GitHub Pages**，等 `deploy` 成功。
4. 访问 <https://chenzhizixin2010.github.io/wrongbook/>。

以后只改 `website/` 下的文件，push 到 `main` 就会自动重新部署。

## 内容维护

- **版本号 / APK 下载地址 / 文件大小 / 发布日期**：页面加载时会调用 GitHub API 读取最新 Release，自动更新。API 被限流或离线时，HTML 里写死的 `v0.12.0` 和下载地址作为回退。发布新版本后无需改页面，但建议偶尔同步一下回退值。
- **截图**：`assets/img/screens/` 下是 720×1280 WebP。原图在 `docs/screens/`，新增截图后按同样尺寸压缩再替换。
- **隐私政策**：`privacy.html` 是当前正式文本；仓库里的 `docs/release/PRIVACY.md` 是早期草稿。权限列表以 App 的 `AndroidManifest.xml` 为准，目前是 CAMERA / INTERNET / ACCESS_NETWORK_STATE / REQUEST_INSTALL_PACKAGES。
- **域名**：如果以后绑定自定义域名，在 `website/` 放 `CNAME` 文件，并同步修改三个 HTML 里的 canonical / OG URL 与 `sitemap.xml`、`robots.txt`。

## 设计约定

- 无外部字体、无 CDN、无统计脚本：国内网络也能完整加载。
- 纸张底色 + 墨水黑 + 批改红；Miuix 相关文案不写渐变，0.9.0 皮肤才提渐变。
- 所有图片都有 `width` / `height` 或固定比例，避免布局跳动；非首屏图片 `loading="lazy"`。
- 支持键盘操作、`prefers-reduced-motion`、跳过导航链接。
