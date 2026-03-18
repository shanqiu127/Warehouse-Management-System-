let echartsModulePromise = null

export const loadECharts = async () => {
  if (!echartsModulePromise) {
    echartsModulePromise = import('./echartsCore').then((mod) => mod.default)
  }

  return echartsModulePromise
}
