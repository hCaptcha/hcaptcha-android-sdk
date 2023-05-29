#!/usr/bin/env node

// - uses: actions/github-script@v6
const fs = require('fs');
const path = require('path');

const validateInput = (inputs) => {
  const usage = "Usage: script [reference-json] [updated-json] [--markdown|--json]"
  Object.entries(inputs).forEach(([inputName, filePath]) => {
    if (!filePath) {
      console.error(usage)
      console.error(`Error: missing input argument ${inputName}`)
      process.exit(1)
    }
    if (!fs.existsSync(filePath)) {
      console.error(usage)
      console.error(`Error: file ${filePath} not exists`)
      process.exit(2)
    }
    // TODO invalid JSON check
  })
}

const benchmarkMapper = (e) => [`${e.className}.${e.name}`, e]

const readBenchmarksFromFile = (filePath) => {
  let content = JSON.parse(fs.readFileSync(filePath))
  return new Map(content.benchmarks.map(benchmarkMapper))
}

const diffNumber = (n) => {
  const sign = Math.sign(n) === 1 ? '+' : ''
  return sign + (Number.isInteger(n) ? n : Number(n).toFixed(2))
}

const report = (referencePath, updatedPath, isMarkdownOutput) => {
  validateInput({ referencePath, updatedPath })

  const reference = readBenchmarksFromFile(referencePath)
  const updated = readBenchmarksFromFile(updatedPath)

  const diff = Array.from(updated, ([k, v]) => {
    let ref = reference.get(k)
    let upd = v

    if (ref === undefined) {
      ref = {
        metrics: {
          timeNs: {
            median: 0
          },
          allocationCount: {
            median: 0
          }
        }
      }
      k += " (New)"
    }

    return [
      k,
      diffNumber((upd.metrics.timeNs.median - ref.metrics.timeNs.median) / (10 ** 6)),
      diffNumber(upd.metrics.allocationCount.median - ref.metrics.allocationCount.median)
    ]
  })

  if (!isMarkdownOutput) {
    return JSON.stringify(diff, null, 2)
  }

  const header = [
    ["Test name", "Time ms. (median)", "Allocations (median)"],
    ["---------", "-----------------", "--------------------"]
  ]
  const markdown = header.concat(diff).map(e => e.join(' | ')).map(e => `| ${e} |`).join('\n');
  return markdown
}

module.exports = { report };

if (require.main === module) {
  const [referencePath, updatedPath, outputType] = process.argv.slice(2)
  const isMarkdownOutput = outputType === "--markdown"
  const result = report(referencePath, updatedPath, isMarkdownOutput)
  console.log(result)
}