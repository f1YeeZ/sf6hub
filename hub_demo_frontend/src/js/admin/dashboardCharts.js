import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

export function createDashboardCharts(elements, data) {
  const charts = []
  if (elements.userGrowth) {
    charts.push(createChart(elements.userGrowth, lineChartOption({
      labels: data.trendDays,
      seriesName: '新增用户',
      values: data.userGrowthSeries,
      color: '#ff4054',
    })))
  }
  if (elements.visitTrend) {
    charts.push(createChart(elements.visitTrend, visitTrendOption(data)))
  }
  if (elements.characterHeat && data.characterComboRanking.length) {
    charts.push(createChart(elements.characterHeat, barChartOption({
      labels: data.characterComboRanking.map(item => item.name),
      values: data.characterComboRanking.map(item => item.value),
    })))
  }
  if (elements.reviewDonut) {
    charts.push(createChart(elements.reviewDonut, reviewDonutOption(data.reviewStatusData)))
  }
  return charts
}

function createChart(element, option) {
  const chart = init(element, 'dark')
  chart.setOption(option)
  return chart
}

function visitTrendOption(data) {
  return {
    backgroundColor: 'transparent',
    color: ['#ff4054', '#31d5e6'],
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 8, textStyle: { color: '#e8d8c8' } },
    grid: { left: 42, right: 16, top: 42, bottom: 30 },
    xAxis: categoryAxis(data.visitTrendDays, false),
    yAxis: valueAxis(),
    series: [
      lineSeries('UV', data.visitUvSeries, 'rgba(255, 64, 84, 0.09)'),
      lineSeries('PV', data.visitPvSeries),
    ],
  }
}

function lineChartOption({ labels, seriesName, values, color }) {
  return {
    backgroundColor: 'transparent',
    color: [color],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 28, bottom: 30 },
    xAxis: categoryAxis(labels, false),
    yAxis: valueAxis(),
    series: [lineSeries(seriesName, values, 'rgba(255, 64, 84, 0.1)')],
  }
}

function reviewDonutOption(reviewStatusData) {
  return {
    backgroundColor: 'transparent',
    color: ['#31d5e6', '#ffc15d', '#69f3a8', '#ff7a87'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#e8d8c8' } },
    series: [{
      name: '审核状态',
      type: 'pie',
      radius: ['58%', '76%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      label: { color: '#e8d8c8' },
      data: reviewStatusData,
    }],
  }
}

function barChartOption({ labels, values }) {
  return {
    backgroundColor: 'transparent',
    color: ['#ff4054'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 78, right: 18, top: 18, bottom: 26 },
    xAxis: valueAxis(),
    yAxis: categoryAxis(labels, true),
    series: [{
      name: '连招数',
      type: 'bar',
      data: values,
      barWidth: 12,
      itemStyle: { borderRadius: 2 },
    }],
  }
}

function categoryAxis(data, inverse) {
  return {
    type: 'category',
    boundaryGap: inverse ? undefined : false,
    inverse,
    data,
    axisLine: { lineStyle: { color: '#3d302e' } },
    axisLabel: { color: inverse ? '#e8d8c8' : '#b9a79c' },
  }
}

function valueAxis() {
  return {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#2b211f' } },
    axisLabel: { color: '#b9a79c' },
  }
}

function lineSeries(name, data, areaColor) {
  return {
    name,
    type: 'line',
    smooth: true,
    showSymbol: false,
    data,
    lineStyle: { width: 2 },
    ...(areaColor ? { areaStyle: { color: areaColor } } : {}),
  }
}
