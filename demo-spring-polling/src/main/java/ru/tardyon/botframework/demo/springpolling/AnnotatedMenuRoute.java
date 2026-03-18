package ru.tardyon.botframework.demo.springpolling;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.Filter;
import ru.tardyon.botframework.dispatcher.FilterResult;
import ru.tardyon.botframework.dispatcher.InnerMiddleware;
import ru.tardyon.botframework.dispatcher.MiddlewareNext;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.dispatcher.annotation.Callback;
import ru.tardyon.botframework.dispatcher.annotation.CallbackPrefix;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.dispatcher.annotation.UseFilters;
import ru.tardyon.botframework.dispatcher.annotation.UseMiddleware;
import ru.tardyon.botframework.message.Buttons;
import ru.tardyon.botframework.message.Keyboards;
import ru.tardyon.botframework.message.Messages;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.ChatType;

/**
 * Annotation-based route demo for menu/callback flow.
 */
@Route(value = "annotated-menu", autoRegister = true)
@UseMiddleware(AnnotatedMenuRoute.TraceMiddleware.class)
public final class AnnotatedMenuRoute {

    @Command("astart")
    public void start(RuntimeContext ctx) {
        ctx.reply(Messages.text("Annotation API: /amenu, /aform, /aecho <text>; Smoke API: /qa, /qa_run_all"));
    }

    @Command("amenu")
    @UseFilters(PrivateChatFilter.class)
    @UseMiddleware(AttemptMiddleware.class)
    public CompletionStage<Void> menu(RuntimeContext ctx, Integer attempt) {
        ctx.reply(
                Messages.text("Аннотационное меню (attempt=" + attempt + ")")
                        .keyboard(Keyboards.inline(k -> k.row(
                                Buttons.callback("Оплатить", "amenu:pay"),
                                Buttons.callback("Помощь", "amenu:help")
                        )))
        );
        return CompletableFuture.completedFuture(null);
    }

    @Callback("amenu:pay")
    public void pay(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        ctx.answerCallback("Платёж подтверждён (annotated)");
        ctx.callbacks().updateCurrentMessage(callback, Messages.markdown("*Платёж:* подтверждён (annotated)"));
    }

    @CallbackPrefix("amenu:")
    public void menuFallback(ru.tardyon.botframework.model.Callback callback, RuntimeContext ctx) {
        if ("amenu:help".equals(callback.data())) {
            ctx.answerCallback("Подсказка отправлена (annotated)");
            ctx.callbacks().updateCurrentMessage(callback, Messages.text("Используйте /astart для навигации"));
            return;
        }
        ctx.answerCallback("Неизвестное действие");
    }

    @ru.tardyon.botframework.dispatcher.annotation.Message(text = "/aecho ", startsWith = true)
    public void echo(Message message, RuntimeContext ctx) {
        String text = message.text() == null ? "" : message.text().substring("/aecho ".length()).trim();
        ctx.reply(Messages.text("Annotated echo: " + text));
    }

    public static final class PrivateChatFilter implements Filter<Message> {
        @Override
        public CompletionStage<FilterResult> test(Message event) {
            if (event == null || event.chat() == null || event.chat().type() != ChatType.PRIVATE) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
            return CompletableFuture.completedFuture(FilterResult.matched());
        }
    }

    public static final class TraceMiddleware implements InnerMiddleware {
        @Override
        public CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next) {
            context.putEnrichment("annotatedRoute", "menu");
            return next.proceed();
        }
    }

    public static final class AttemptMiddleware implements InnerMiddleware {
        @Override
        public CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next) {
            context.putEnrichment("attempt", 1);
            return next.proceed();
        }
    }
}
