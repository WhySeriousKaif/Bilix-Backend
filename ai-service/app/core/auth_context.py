from contextvars import ContextVar, Token


_user_authorization: ContextVar[str | None] = ContextVar(
    "billix_user_authorization",
    default=None,
)


def set_user_authorization(value: str) -> Token[str | None]:
    """Bind one user's authorization header to the current async request."""
    return _user_authorization.set(value)


def reset_user_authorization(token: Token[str | None]) -> None:
    """Remove request credentials after the assistant response is complete."""
    _user_authorization.reset(token)


def get_user_authorization() -> str:
    value = _user_authorization.get()
    if value is None:
        raise RuntimeError("User authorization is not available for this request.")
    return value
