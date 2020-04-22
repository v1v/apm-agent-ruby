#!/usr/bin/env groovy
@Library('apm@current') _

pipeline {
  agent { label 'linux && immutable' }
  options {
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20', daysToKeepStr: '30'))
    timestamps()
    ansiColor('xterm')
    disableResume()
    durabilityHint('PERFORMANCE_OPTIMIZED')
    rateLimitBuilds(throttle: [count: 60, durationName: 'hour', userBoost: true])
    quietPeriod(10)
  }
  triggers {
    issueCommentTrigger('(?i).*jenkins\\W+run\\W+the\\W+linters(?:\\W+please)?.*')
  }
  stages {
    stage('Sanity checks') {
      environment {
        HOME = "${env.WORKSPACE}"
        PATH = "${env.WORKSPACE}/bin:${env.PATH}"
      }
      steps {
        script {
          env.GIT_SHA = getGitCommitSha()
          preCommit(commit: "${env.GIT_SHA}", junit: true)
        }
      }
    }
    stage('Prepare Rubocop') {
      steps {
        dir('rubocop') {
          git credentialsId: '2a9602aa-ab9f-4e52-baf3-b71ca88469c7-UserAndToken',
              url: 'https://github.com/mikker/rubocop-action.git'
          sh 'docker build --tag rubocop .'
        }
        script {
          def json = readJSON text: '{ "repository": { "name": "apm-agent-ruby", owner: { "login": "elastic" } } }'
          writeJSON file: '.event.json', json: json, pretty: 4
        }
        withEnv(["GITHUB_TOKEN=123", "GITHUB_WORKSPACE=/app" ]) {
          sh "docker run --rm -t -v ${env.WORKSPACE}:/${env.GITHUB_WORKSPACE} \
                    -e GITHUB_EVENT_PATH=/app/.event.json \
                    -e GITHUB_SHA=${env.GIT_SHA} \
                    -e GITHUB_WORKSPACE=${env.GITHUB_WORKSPACE} \
                    rubocop"
        }
      }
    }
  }
}
