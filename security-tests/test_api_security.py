"""Small black-box security regression suite for a running Bilix API."""

import os

import pytest
import requests


BASE_URL = os.getenv("BILIX_BASE_URL", "http://localhost:5001").rstrip("/")
EMAIL = os.getenv("BILIX_TEST_EMAIL", "cashier@molla.com")
PASSWORD = os.getenv("BILIX_TEST_PASSWORD")
OWN_BRANCH_ID = os.getenv("BILIX_OWN_BRANCH_ID", "1")
OTHER_BRANCH_ID = os.getenv("BILIX_OTHER_BRANCH_ID")


@pytest.fixture(scope="session")
def jwt():
    assert PASSWORD, "Set BILIX_TEST_PASSWORD for the test cashier before running authenticated checks"
    response = requests.post(
        f"{BASE_URL}/auth/login",
        json={"email": EMAIL, "password": PASSWORD},
        timeout=10,
    )
    assert response.status_code == 200, response.text
    token = response.json().get("jwt")
    assert token, "Login response did not include a JWT"
    return token


def auth_header(token):
    return {"Authorization": f"Bearer {token}"}


def test_protected_endpoint_rejects_missing_jwt():
    response = requests.get(
        f"{BASE_URL}/api/orders/branch/{OWN_BRANCH_ID}", timeout=10
    )
    assert response.status_code == 401


def test_protected_endpoint_rejects_invalid_jwt():
    response = requests.get(
        f"{BASE_URL}/api/orders/branch/{OWN_BRANCH_ID}",
        headers=auth_header("invalid.jwt.value"),
        timeout=10,
    )
    assert response.status_code == 401


def test_valid_cashier_can_access_assigned_branch(jwt):
    response = requests.get(
        f"{BASE_URL}/api/orders/branch/{OWN_BRANCH_ID}",
        headers=auth_header(jwt),
        timeout=10,
    )
    assert response.status_code == 200, response.text


def test_refund_rejects_missing_required_fields(jwt):
    """Input validation: the server rejects an incomplete refund instead of trusting it."""
    response = requests.post(
        f"{BASE_URL}/api/refunds",
        json={},
        headers=auth_header(jwt),
        timeout=10,
    )
    assert response.status_code == 400
    assert response.json().get("error") == "VALIDATION_ERROR"


def test_refund_rejects_negative_quantity(jwt):
    """Input validation: a negative quantity must never reach the refund service."""
    response = requests.post(
        f"{BASE_URL}/api/refunds",
        json={
            "orderId": 1,
            "reason": "negative quantity probe",
            "paymentType": "CASH",
            "items": [{"orderItemId": 1, "quantity": -5}],
        },
        headers=auth_header(jwt),
        timeout=10,
    )
    assert response.status_code == 400
    assert response.json().get("error") == "VALIDATION_ERROR"


@pytest.mark.skipif(
    not OTHER_BRANCH_ID,
    reason="Set BILIX_OTHER_BRANCH_ID to a branch outside the test cashier's scope",
)
def test_cashier_cannot_read_another_branch_refunds(jwt):
    """Second resource-level check: the same boundary must hold for refunds, not only orders."""
    response = requests.get(
        f"{BASE_URL}/api/refunds/branch/{OTHER_BRANCH_ID}",
        headers=auth_header(jwt),
        timeout=10,
    )
    assert response.status_code == 403
    assert "do not have access" in response.json().get("message", "")


def test_public_signup_cannot_create_super_admin():
    response = requests.post(
        f"{BASE_URL}/auth/signup",
        json={
            "fullName": "Security Test",
            "email": "security-admin-attempt@example.com",
            "password": "StrongPassword123!",
            "role": "ROLE_ADMIN",
        },
        timeout=10,
    )
    assert response.status_code == 400
    assert "Cannot self-register" in response.json().get("message", "")


@pytest.mark.skipif(
    not OTHER_BRANCH_ID,
    reason="Set BILIX_OTHER_BRANCH_ID to a branch outside the test cashier's scope",
)
def test_cashier_cannot_access_another_branch(jwt):
    response = requests.get(
        f"{BASE_URL}/api/orders/branch/{OTHER_BRANCH_ID}",
        headers=auth_header(jwt),
        timeout=10,
    )
    assert response.status_code == 403
    assert "do not have access" in response.json().get("message", "")
