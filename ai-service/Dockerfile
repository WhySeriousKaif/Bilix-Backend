FROM python:3.12-slim

WORKDIR /app

COPY pyproject.toml ./
COPY app app
COPY data data
COPY scripts scripts

RUN pip install --no-cache-dir .

EXPOSE 10000

CMD ["sh", "-c", "if [ ! -f data/rag/policy_index.joblib ]; then python scripts/build_policy_index.py; fi && exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-10000}"]
