# Filter Contract

## Interface

```java
public interface Filter<TEvent> {
    CompletionStage<FilterResult> test(TEvent event);
    default CompletionStage<FilterResult> test(TEvent event, RuntimeContext context) { ... }
}
```

## Result model

`FilterResult` содержит статус:

- `MATCHED` (+ optional enrichment map)
- `NOT_MATCHED`
- `FAILED` (+ throwable)

## Composition

Поддерживается композиция:

- `and(...)`
- `or(...)`
- `not()`
- `Filter.any()`
- `Filter.of(Predicate<TEvent>)`

## Built-in filters

Message:

- `BuiltInFilters.command(...)`
- `BuiltInFilters.textEquals(...)`
- `BuiltInFilters.textStartsWith(...)`
- `BuiltInFilters.chatType(...)`
- `BuiltInFilters.fromUser(...)`
- `BuiltInFilters.hasAttachment()`
- `BuiltInFilters.state(...)`, `stateIn(...)`

Callback:

- `BuiltInFilters.fromCallbackUser(...)`
- `BuiltInFilters.callbackDataPresent()`
- `BuiltInFilters.callbackDataEquals(...)`
- `BuiltInFilters.callbackDataStartsWith(...)`

## Enrichment contract

- Filter может добавить enrichment (`Map<String,Object>`).
- Enrichment доступен downstream в `RuntimeContext` и DI resolution.
- Key conflicts между filter/middleware данными запрещены.
