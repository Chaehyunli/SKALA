import re

PASSWORD_PATTERN = re.compile(
    r"^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':\"\\|,.<>/?]).{8,}$"
)


def is_valid_password(password: str) -> bool:
    return bool(PASSWORD_PATTERN.match(password))


if __name__ == "__main__":
    test_passwords = [
        "Abcdefg1!",
        "abcdefg1!",
        "ABCDEFG1!",
        "Abcdefgh!",
        "Abcdefg1",
        "Ab1!",
    ]

    for pw in test_passwords:
        result = "유효함" if is_valid_password(pw) else "유효하지 않음"
        print(f"{pw}: {result}")
