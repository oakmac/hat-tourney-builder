#! /usr/bin/env node

// This script creates a build in the 00_build/ folder ready for public usage.

const assert = require('assert')
const fs = require('fs')
const path = require('path')
const shell = require('shelljs')

// collect some information about the release
const gitFullHash = shell.exec('git rev-parse HEAD', { silent: true }).stdout.trim()
const gitShortHash = gitFullHash.substr(0, 10)

// create the releaseId
const buildStartTime = new Date()
const releaseId = createReleaseId(buildStartTime, gitShortHash)

const projectRoot = path.join(__dirname, '..')

// remove any previous build
const buildDir = path.join(projectRoot, '00_build')
shell.rm('-rf', buildDir)

// compile CLJS
const cljsBuild = shell.exec('npm run build', { cwd: projectRoot, fatal: true, silent: false })
if (cljsBuild.code !== 0) {
  console.error('Failed to build ClojureScript. Goodbye!')
  process.exit(1)
}

// hash assets
const sriHash = shell.exec('cat public/js/main.js | openssl dgst -sha384 -binary | openssl base64 -A', { cwd: projectRoot, fatal: true, silent: false })
const fileHashResult = shell.exec('shasum --algorithm 256 public/js/main.js', { cwd: projectRoot, fatal: true, silent: false })
if (sriHash.code !== 0 || fileHashResult.code !== 0) {
  console.error('Failed to create asset hashes. Goodbye!')
  process.exit(1)
}
const fileHash32 = fileHashResult.substring(0, 32)
const jsFilename = 'main.' + fileHash32 + '.js'

// copy files to 00_build/ directory
const buildIndexFile = path.join(buildDir, 'index.html')
const buildJsFile = path.join(buildDir, 'js', jsFilename)
shell.exec('mkdir -p ' + path.join(buildDir, 'js'))
shell.cp(path.join(projectRoot, 'public/index.html'), buildIndexFile)
shell.cp(path.join(projectRoot, 'public/js/main.js'), buildJsFile)

// inject build-id, subresource integrity hash to index.html
const indexFileContents = fs.readFileSync(buildIndexFile, 'utf8')
const hashedIndexFileContents = indexFileContents.replace(
  '<script src="js/main.js">',
  '<script src="js/' + jsFilename + '" integrity="sha384-' + sriHash.stdout + '">'
).replace(
  '$$release-id$$',
  releaseId
)
fs.writeFileSync(buildIndexFile, hashedIndexFileContents)

console.log('\nCreated build at', buildDir, 'with releaseId:', releaseId)

// -----------------------------------------------------------------------------
// Helpers

function releaseTimestamp (d) {
  // always use US Central Time zone for the releaseId timestamp
  const timeOptions = {
    day: '2-digit',
    hour12: false,
    month: '2-digit',
    timeZone: 'America/Chicago',
    year: 'numeric'
  }
  const centralUSTimeStr = d.toLocaleTimeString('en-US', timeOptions)
  const year = centralUSTimeStr.substring(6, 10)
  const month = centralUSTimeStr.substring(0, 2)
  const day = centralUSTimeStr.substring(3, 5)
  const hours = centralUSTimeStr.substring(12, 14)
  const minutes = centralUSTimeStr.substring(15, 17)
  const seconds = centralUSTimeStr.substring(18, 20)

  const timePart = year + '-' + month + '-' + day + '-' + hours + minutes + seconds

  // sanity-check:
  const timePartRegex = /^\d{4}-\d{2}-\d{2}-\d{6}$/
  assert(timePartRegex.test(timePart), 'releaseId time part is broken!')

  return timePart
}

function createReleaseId (currentTime, shortHash) {
  return releaseTimestamp(currentTime) + '.' + shortHash
}
