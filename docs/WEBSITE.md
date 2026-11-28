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

线上地址：<https://chenzhizixin2010.github.io/wrongbook/>

当前 Pages 的源是 **`gh-pages` 分支根目录**（Deploy from a branch）。`.github/workflows/pages.yml` 会在 `main` 上的 `website/**` 有变更时，把 `website/` 子树推到 `gh-pages`，触发 Pages 重新构建；不需要手动操作。

以后只改 `website/` 下的文件，push 到 `main` 就会自动重新部署。

如果 Actions 不可用，可以手动发布：

```powershell
$env:Path = "D:\dev-tools\git\cmd;" + $env:Path
git subtree push --prefix website origin gh-pages
```

如果 Pages 被关闭，在 **Settings → Pages → Build and deployment** 里选择 **Deploy from a branch**，分支选 `gh-pages`，目录选 `/ (root)`，保存后等待构建完成。

## 内容维护

- **版本号 / APK 下载地址 / 文件大小 / 发布日期**：页面加载时会调用 GitHub API 读取最新 Release，自动更新。API 被限流或离线时，HTML 里写死的 `v0.13.0` 和下载地址作为回退。发布新版本后无需改页面，但建议偶尔同步一下回退值。
- **截图**：`assets/img/screens/` 下是 720×1280 WebP。原图在 `docs/screens/`，新增截图后按同样尺寸压缩再替换。
- **隐私政策**：`privacy.html` 是当前正式文本；仓库里的 `docs/release/PRIVACY.md` 是早期草稿。权限列表以 App 的 `AndroidManifest.xml` 为准，目前是 CAMERA / INTERNET / ACCESS_NETWORK_STATE / REQUEST_INSTALL_PACKAGES。
- **域名**：如果以后绑定自定义域名，在 `website/` 放 `CNAME` 文件，并同步修改三个 HTML 里的 canonical / OG URL 与 `sitemap.xml`、`robots.txt`。

## 设计约定

- 无外部字体、无 CDN、无统计脚本：国内网络也能完整加载。
- 纸张底色 + 墨水黑 + 批改红；Miuix 相关文案不写渐变，0.9.0 皮肤才提渐变。
- 所有图片都有 `width` / `height` 或固定比例，避免布局跳动；非首屏图片 `loading="lazy"`。
- 支持键盘操作、`prefers-reduced-motion`、跳过导航链接。
