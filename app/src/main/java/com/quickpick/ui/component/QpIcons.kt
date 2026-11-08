package com.quickpick.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自绘极简线条图标（v1.1.7 §9/10：用户嫌 emoji 图标老气，不引新依赖）。
 * 24dp viewport，1.8dp 描边圆角线帽，material symbols 风格。
 */
object QpIcons {

    private fun builder() = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    )

    /** 加号（新建） */
    val Plus: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, pathFillType = PathFillType.NonZero
    ) {
        moveTo(12f, 5f); lineTo(12f, 19f)
        moveTo(5f, 12f); lineTo(19f, 12f)
    }.build()

    /** 四芒星（AI 生成） */
    val Sparkle: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        // 直线四芒星（cubicTo 在当前 path DSL 不可用，菱形星同样清晰）
        moveTo(12f, 3f); lineTo(15f, 9.8f); lineTo(21f, 12f); lineTo(15f, 14.2f)
        lineTo(12f, 21f); lineTo(9f, 14.2f); lineTo(3f, 12f); lineTo(9f, 9.8f)
        close()
    }.build()

    /** 下载箭头入托盘（导入） */
    val Download: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(12f, 4f); lineTo(12f, 14f)
        moveTo(7.5f, 10.5f); lineTo(12f, 15f); lineTo(16.5f, 10.5f)
        moveTo(4f, 19f); lineTo(20f, 19f)
    }.build()

    /** 上传箭头出托盘（导出） */
    val Upload: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(12f, 15f); lineTo(12f, 5f)
        moveTo(7.5f, 8.5f); lineTo(12f, 4f); lineTo(16.5f, 8.5f)
        moveTo(4f, 19f); lineTo(20f, 19f)
    }.build()

    // ============ 首屏隐私三条（v1.1.15）：替代 ✅ emoji，跟主题色走 ============

    /** 手机轮廓（数据只在设备本地） */
    val Phone: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        // 机身：竖矩形（当前 path DSL 无圆弧，用直角近似）
        moveTo(7.5f, 3f); lineTo(16.5f, 3f); lineTo(16.5f, 21f); lineTo(7.5f, 21f); close()
        // home 指示条
        moveTo(10.5f, 18f); lineTo(13.5f, 18f)
    }.build()

    /** 上传箭头 + 斜杠（不上传到服务器） */
    val UploadOff: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(12f, 15f); lineTo(12f, 6.5f)
        moveTo(9f, 9.5f); lineTo(12f, 6.5f); lineTo(15f, 9.5f)
        moveTo(5f, 19f); lineTo(19f, 19f)
        // 禁止斜杠：左上一个点、右下收尾
        moveTo(4.5f, 4f); lineTo(19.5f, 20f)
    }.build()

    /** 盾牌 + 对勾（不收集隐私） */
    val ShieldCheck: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(12f, 3f); lineTo(19f, 6f); lineTo(19f, 12.5f); lineTo(12f, 20.5f)
        lineTo(5f, 12.5f); lineTo(5f, 6f); close()
        moveTo(8.8f, 11.9f); lineTo(11.2f, 14.3f); lineTo(15.4f, 9.7f)
    }.build()

    /** 文档轮廓（本地文件） */
    val FileDoc: ImageVector = builder().path(
        stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero
    ) {
        // 纸形：左上起，右上折角
        moveTo(7f, 3.5f); lineTo(13.5f, 3.5f); lineTo(17f, 7f); lineTo(17f, 20.5f)
        moveTo(7f, 3.5f); lineTo(7f, 20.5f); lineTo(17f, 20.5f)
        moveTo(13.5f, 3.5f); lineTo(13.5f, 7f); lineTo(17f, 7f)
    }.build()
}
