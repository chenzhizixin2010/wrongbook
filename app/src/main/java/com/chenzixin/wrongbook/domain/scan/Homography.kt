package com.chenzixin.wrongbook.domain.scan

/**
 * 单应性变换（透视变换）的纯数学实现。
 *
 * 用途：把用户拖拽出的任意四边形，映射成矩形输出。
 * 这里只提供「单位正方形 → 四边形」的方向，因为实际裁剪是**反向映射**：
 * 目标图每个像素 (u,v) 反查源图坐标，再采样 —— 这样输出不会出现空洞。
 *
 * 不依赖 Android，可直接 JVM 单测。
 */
class Homography private constructor(private val m: DoubleArray) {

    /** 把 (x,y) 映射到目标坐标；分母接近 0 时返回 null（点在无穷远）。 */
    fun map(x: Double, y: Double): Pair<Double, Double>? {
        val w = m[6] * x + m[7] * y + 1.0
        if (kotlin.math.abs(w) < 1e-12) return null
        val nx = (m[0] * x + m[1] * y + m[2]) / w
        val ny = (m[3] * x + m[4] * y + m[5]) / w
        return nx to ny
    }

    companion object {

        /**
         * @param quad 4 个点，顺序必须是左上、右上、右下、左下
         * @return 退化四边形（面积≈0）返回 null，调用方据此放弃裁剪
         */
        fun fromUnitSquareTo(quad: List<Pair<Double, Double>>): Homography? {
            if (quad.size != 4) return null

            // 8 个未知数：h11 h12 h13 h21 h22 h23 h31 h32，h33 固定为 1
            val a = Array(8) { DoubleArray(9) }
            val unit = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0)
            quad.forEachIndexed { i, (x, y) ->
                val (u, v) = unit[i]
                a[i * 2] = doubleArrayOf(u, v, 1.0, 0.0, 0.0, 0.0, -x * u, -x * v, x)
                a[i * 2 + 1] = doubleArrayOf(0.0, 0.0, 0.0, u, v, 1.0, -y * u, -y * v, y)
            }

            val solved = gaussianSolve(a) ?: return null
            if (solved.any { !it.isFinite() }) return null

            // 面积校验：三点共线时变换没有意义
            if (area(quad) < 1e-6) return null
            return Homography(solved)
        }

        private fun area(p: List<Pair<Double, Double>>): Double {
            var sum = 0.0
            for (i in p.indices) {
                val (x1, y1) = p[i]
                val (x2, y2) = p[(i + 1) % p.size]
                sum += x1 * y2 - x2 * y1
            }
            return kotlin.math.abs(sum) / 2.0
        }

        /** 高斯消元（带部分主元），解 8×8 线性系统。 */
        private fun gaussianSolve(matrix: Array<DoubleArray>): DoubleArray? {
            val n = matrix.size
            for (col in 0 until n) {
                var pivot = col
                for (row in col + 1 until n) {
                    if (kotlin.math.abs(matrix[row][col]) > kotlin.math.abs(matrix[pivot][col])) pivot = row
                }
                if (kotlin.math.abs(matrix[pivot][col]) < 1e-10) return null
                val tmp = matrix[col]
                matrix[col] = matrix[pivot]
                matrix[pivot] = tmp

                val div = matrix[col][col]
                for (j in col..n) matrix[col][j] /= div

                for (row in 0 until n) {
                    if (row == col) continue
                    val factor = matrix[row][col]
                    if (factor == 0.0) continue
                    for (j in col..n) matrix[row][j] -= factor * matrix[col][j]
                }
            }
            return DoubleArray(n) { matrix[it][n] }
        }
    }
}
