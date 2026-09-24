FROM golang:1.25-alpine3.23 AS builder

SHELL ["/bin/ash", "-o", "pipefail", "-c"]

COPY doc-generation /doc-generation

WORKDIR /doc-generation
RUN mkdir -p /docs/description
RUN set -e; \
    version=2.13.2; \
    os=$(go env GOOS); \
    arch=$(go env GOARCH); \
    file="golangci-lint-${version}-${os}-${arch}.tar.gz"; \
    base="https://github.com/golangci/golangci-lint/releases/download/v${version}"; \
    wget -q -O "/tmp/${file}" "${base}/${file}"; \
    wget -q -O /tmp/checksums.txt "${base}/golangci-lint-${version}-checksums.txt"; \
    grep " ${file}\$" /tmp/checksums.txt | sed "s#${file}\$#/tmp/${file}#" | sha256sum -c -; \
    tar -xzf "/tmp/${file}" -C /tmp; \
    mv "/tmp/golangci-lint-${version}-${os}-${arch}/golangci-lint" /usr/local/bin/golangci-lint
RUN go run main.go -docFolder=../docs

FROM alpine:3.23

COPY --from=builder /docs /docs
COPY docs/tool-description.md /docs/
COPY entry.sh /

RUN adduser -u 2004 -D docker && chown -R docker:docker /docs

USER docker

ENTRYPOINT [ "sh", "/entry.sh" ]
