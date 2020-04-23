# frozen_string_literal: true

require 'bundler/gem_tasks'

desc """Post release action:
Update `test-3.x` branch to be at released commit and push it to GitHub.
"""
namespace :release do
  task :update_branch do
    `git checkout test-3.x &&
    git rebase master &&
    git push origin test-3.x &&
    git checkout master`
  end
end
