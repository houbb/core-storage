我认为 **P4 Image Runtime** 是 `core-storage` 第一个真正体现"平台价值"的 Runtime。

前面的 P0~P3 都属于基础设施。

而 **P4 开始提供业务能力（Business Capability）**。

几乎所有平台最终都会有图片需求：

* 用户头像
* 企业 Logo
* Banner
* Markdown 图片
* AI 图片
* 商品图片
* 图标（Icon）
* 截图
* 海报
* 封面
* 缩略图
* 水印
* OCR
* 二维码

如果每个业务自己处理图片，很快就会出现几十套重复实现。

所以应该建立统一的：

> **Image Runtime（图片运行时）**

不是 Image Service。

因为它负责的是：

> **图片整个生命周期（Image Lifecycle）**

---

# Phase 4：Image Runtime ⭐⭐⭐⭐⭐

> **目标：建立统一图片处理平台，所有图片上传、转换、压缩、裁剪、预览、缩略图等能力全部统一。**

一句话：

> **任何图片，都应该经过 Image Runtime。**

以后：

```text
Browser

↓

Image Runtime

↓

Image Pipeline

↓

Storage Runtime

↓

Driver
```

而不是：

```java
ImageIO.read()

Thumbnailator...

```

到处都是。

---

# 一、为什么 Image Runtime？

举个例子。

头像：

需要：

```text
Resize

200x200
```

Markdown：

需要：

```text
Compress
```

Logo：

需要：

```text
PNG

透明背景
```

AI图片：

需要：

```text
WebP

1024
```

Banner：

需要：

```text
16:9
```

如果：

全部：

业务自己写：

以后：

几十套。

统一：

Image Runtime。

---

# 二、整体架构

```text
                Upload

                  │

            Image Runtime

                  │

      ┌───────────┴────────────┐

 Metadata                 Image Pipeline

                                │

      Resize

      Compress

      Crop

      Rotate

      Watermark

      Thumbnail

                                │

                     Storage Runtime
```

整个：

图片：

只有：

Image Runtime。

---

# 三、统一对象模型

新增：

```java
ImageResource
```

继承：

```text
StorageResource

↓

ImageResource
```

增加：

```text
Width

Height

Format

ColorSpace

Alpha

Orientation
```

以后：

图片：

都是：

ImageResource。

---

# 四、图片生命周期

```text
Upload

↓

Metadata

↓

Analyze

↓

Pipeline

↓

Thumbnail

↓

Storage

↓

Preview

↓

Access
```

这里：

重点：

Analyze。

上传以后：

立即：

解析：

图片信息。

---

# 五、数据库设计

## storage_image

建议：

```sql
CREATE TABLE storage_image
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    resource_uuid VARCHAR(64),

    width INT,

    height INT,

    format VARCHAR(32),

    color_space VARCHAR(32),

    has_alpha BOOLEAN,

    orientation INT,

    dpi INT,

    thumbnail_ready BOOLEAN,

    preview_ready BOOLEAN,

    create_time DATETIME
);
```

这里只保存：

图片信息。

真正：

Metadata：

还是：

P1。

---

## storage_image_variant

建议：

```sql
CREATE TABLE storage_image_variant
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    image_uuid VARCHAR(64),

    variant_name VARCHAR(64),

    metadata_uuid VARCHAR(64),

    width INT,

    height INT,

    format VARCHAR(16),

    create_time DATETIME
);
```

这是整个：

Image Runtime：

最重要的一张表。

例如：

原图：

```text
origin
```

缩略图：

```text
thumb
```

中图：

```text
medium
```

WebP：

```text
webp
```

AVIF：

```text
avif
```

全部：

Variant。

以后：

不会：

覆盖原图。

---

# 六、Image Variant

建议：

统一：

```java
enum Variant
```

```text
ORIGINAL

THUMBNAIL

SMALL

MEDIUM

LARGE

WEB

WEBP

AVIF
```

以后：

前端：

直接：

请求：

```text
avatar

↓

thumbnail
```

不用：

压图。

---

# 七、Image Pipeline

整个：

Image：

处理：

统一：

Pipeline。

例如：

```text
Upload

↓

Read

↓

Metadata

↓

Resize

↓

Compress

↓

Convert

↓

Watermark

↓

Save
```

以后：

新增：

OCR：

二维码：

不用：

改架构。

---

# 八、图片能力

P4：

建议：

支持：

### Resize

```text
200x200

1024x1024
```

---

### Crop

```text
Center

Top

Custom
```

---

### Compress

质量：

```text
100

90

80

70
```

---

### Convert

```text
PNG

JPEG

WEBP

AVIF
```

---

### Rotate

```text
90

180

270
```

---

### Flip

```text
Horizontal

Vertical
```

---

### Thumbnail

自动：

生成。

---

### Metadata

自动：

读取：

```text
Width

Height

Size

Format
```

---

# 九、API

上传：

```http
POST

/images
```

获取：

```http
GET

/images/{uuid}
```

获取：

缩略图：

```http
GET

/images/{uuid}/thumbnail
```

Variant：

```http
GET

/images/{uuid}/variant/{name}
```

转换：

```http
POST

/images/{uuid}/convert
```

压缩：

```http
POST

/images/{uuid}/compress
```

裁剪：

```http
POST

/images/{uuid}/crop
```

---

# 十、前端 UX

资源中心：

新增：

Image。

点击：

```text
图片

↓

预览
```

不是：

下载。

---

图片详情：

```text
┌──────────────────────────────────────────────┐

               图片预览

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1024 × 768

PNG

245 KB

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Variants

Original

Thumbnail

WebP

Medium

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

下载

复制URL

复制UUID

删除

└──────────────────────────────────────────────┘
```

---

点击：

Variant：

立即：

切换。

---

# 十一、交互设计

图片资源默认采用**视觉优先**的体验，而不是文件列表。

## 图片中心

```text
┌──────────────────────────────────────────────┐

🔍 搜索图片

过滤：

类型

尺寸

格式

──────────────────────────────────────────────

🖼 Avatar

1024×1024

PNG

──────────────────────────────────────────────

🖼 Banner

1920×1080

JPEG

──────────────────────────────────────────────

🖼 Logo

512×512

SVG

```

默认：

卡片布局。

不是：

表格。

---

点击：

进入：

图片详情。

左侧：

```text
图片预览
```

右侧：

```text
基础信息

尺寸

格式

大小

颜色空间

透明通道

Orientation

Variants

Metadata

引用关系
```

底部：

快捷操作：

```text
裁剪

压缩

生成缩略图

格式转换

下载
```

---

上传图片：

建议：

采用：

```text
拖拽

↓

实时预览

↓

自动分析

↓

填写名称

↓

完成
```

上传后：

立即：

显示：

```text
图片：

1024×768

PNG

245KB

缩略图已生成

WebP 已生成
```

而不是：

上传结束：

什么都不知道。

---

# 十二、为什么现在不做 AI 图片？

很多人：

会：

把：

OCR：

背景去除：

AI修图：

放进：

Image Runtime。

我建议：

不要。

原因：

Image Runtime：

负责：

图片处理。

不是：

AI。

以后：

新增：

```text
core-ai-image
```

即可。

Image Runtime：

提供：

统一：

Pipeline。

AI：

只是：

Pipeline：

一个节点。

---

# 十三、P4 核心设计原则（必须坚持）

Image Runtime 的目标不是做一个图片编辑器，而是成为整个平台的图片基础设施。

因此需要坚持以下原则：

1. **原图永远不可修改。** 所有压缩、裁剪、格式转换都生成新的 Variant，而不是覆盖原图。
2. **图片处理采用 Pipeline 模式。** 每个处理步骤（Resize、Compress、Convert、Watermark）都是独立节点，可自由组合。
3. **Variant 是一等公民。** 缩略图、WebP、AVIF、中图、大图都通过统一 Variant 模型管理，而不是散落在磁盘目录中。
4. **Image Runtime 只负责图片处理，不负责 AI 推理。** OCR、背景抠图、超分辨率、AI 修复等未来通过 `core-ai` 或扩展 Pipeline 接入。
5. **所有图片仍然属于 `StorageResource`。** Image Runtime 是 Resource Runtime 的垂直扩展，而不是独立的存储系统。

---

# Image Runtime 在整个 `core-storage` 中的位置

经过 P4 后，整个架构会变得非常清晰：

```text
                 Resource Runtime
                        │
        ┌───────────────┼────────────────┐
        │               │                │
 Metadata Runtime  Access Runtime  Image Runtime
        │               │                │
        └───────────────┼────────────────┘
                        │
                 Storage Runtime
                        │
                 Storage Driver
                        │
     Local / Database / MinIO / OSS / S3
```

这里可以看到，**Image Runtime 并不是替代 Storage，而是建立在 `StorageResource` 之上的第一个资源专用 Runtime**。未来视频（Video Runtime）、文档（Document Runtime）、AI 模型（Model Runtime）都可以沿用同样的扩展模式，而整个 `core-storage` 始终保持统一的资源模型。
