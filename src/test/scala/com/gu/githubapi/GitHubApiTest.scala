package com.gu.githubapi

import org.scalatest.FunSuite

class GitHubApiTest extends FunSuite {

  test("The deployments JSON from GitHub should parse correctly") {
    val fakeResponseBody = """{"data":{"repository":{"deployments":{"edges":[{"node":{"databaseId":123,"createdAt":"2020-04-27T08:04:15Z","environment":"external-beta","state":"ABANDONED","payload":null,"latestStatus":null}},{"node":{"databaseId":124,"createdAt":"2020-04-27T08:19:08Z","environment":"external-beta","state":"ABANDONED","payload":null,"latestStatus":null}},{"node":{"databaseId":125,"createdAt":"2020-04-27T08:27:46Z","environment":"external-beta","state":"PENDING","payload":null,"latestStatus":{"createdAt":"2020-04-27T08:30:46Z","description":"17797"}}}]}}}}"""
    val result = GitHubApi.extractDeployments(fakeResponseBody)
    assert(result.isRight)
  }

}
