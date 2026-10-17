# from django.test import TestCase

# # Create your tests here.

from django.contrib.auth import get_user_model
from rest_framework.authtoken.models import Token
from rest_framework.test import APITestCase


class PostAPITest(APITestCase):

    def setUp(self):
        User = get_user_model()

        self.admin_user = User.objects.create_superuser(
            username="testadmin",
            email="test@example.com",
            password="TestPassword123!"
        )

        self.token = Token.objects.create(user=self.admin_user)
        self.url = "/api_root/Post/"

    def test_get_without_authentication(self):
        """인증 없이 GET 요청 시 접근 차단"""
        response = self.client.get(self.url)

        self.assertIn(response.status_code, [401, 403])

    def test_get_with_token(self):
        """관리자 토큰으로 GET 요청 시 성공"""
        self.client.credentials(
            HTTP_AUTHORIZATION=f"Token {self.token.key}"
        )

        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertIsInstance(response.data, list)

    def test_post_without_authentication(self):
        """인증 없이 POST 요청 시 접근 차단"""
        response = self.client.post(
            self.url,
            {
                "title": "자동 테스트 게시물",
                "text": "API 테스트"
            },
            format="json"
        )

        self.assertIn(response.status_code, [401, 403])