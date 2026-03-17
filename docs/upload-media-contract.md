# Upload Media Contract

## Upload primitives

- `InputFile` (`fromPath`, `fromBytes`, `fromStream`)
- `UploadService`
- `UploadPreparation`, `UploadResult`, `UploadRef`

## Transfer modes

- multipart upload
- resumable upload

## Runtime integration

- `Dispatcher.withUploadService(...)` подключает upload runtime.
- `RuntimeContext.media()` и media shortcuts требуют upload + bot client.

## Media API

`MediaMessagingFacade` поддерживает send/reply операций для image/file/video/audio.
