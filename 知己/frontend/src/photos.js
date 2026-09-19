/**
 * 图片墙数据源。
 *
 * 把图片文件丢进 frontend/src/assets/gallery/ 就会自动出现在轮播图和图片墙里，
 * 不需要改代码（按文件名排序）。支持 jpg / jpeg / png / webp / avif / gif。
 */
const modules = import.meta.glob('./assets/gallery/*.{jpg,jpeg,png,webp,avif,gif}', {
  eager: true,
  import: 'default'
})

export const photos = Object.entries(modules)
  .sort(([a], [b]) => a.localeCompare(b, 'zh-Hans-CN'))
  .map(([path, src]) => ({
    src,
    name: path.split('/').pop().replace(/\.[^.]+$/, '')
  }))
