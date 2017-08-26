package org.flips.advice;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.flips.annotation.FlipBeanWith;
import org.flips.exception.FeatureNotEnabledException;
import org.flips.exception.FlipWithBeanFailedException;
import org.flips.fixture.TestClientFlipBeanSpringComponentSource;
import org.flips.fixture.TestClientFlipBeanSpringComponentTarget;
import org.flips.utils.AnnotationUtils;
import org.flips.utils.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AnnotationUtils.class, Utils.class})
public class FlipBeanWithAdviceUnitTest {

    @InjectMocks
    private FlipBeanAdvice flipBeanAdvice;

    @Mock
    private ApplicationContext applicationContext;

    @Test
    public void shouldNotFlipBeanAsTargetClassToBeFlippedWithIsSameAsSourceClass() throws Throwable {
        ProceedingJoinPoint joinPoint         = mock(ProceedingJoinPoint.class);
        MethodSignature signature             = mock(MethodSignature.class);
        Method method                         = TestClientFlipBeanSpringComponentSource.class.getMethod("map", String.class);
        FlipBeanWith flipBeanWith             = mock(FlipBeanWith.class);
        Class tobeFlippedWith                 = TestClientFlipBeanSpringComponentSource.class;

        PowerMockito.mockStatic(AnnotationUtils.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(AnnotationUtils.findAnnotation(method.getDeclaringClass(), FlipBeanWith.class)).thenReturn(flipBeanWith);
        when(flipBeanWith.alternateBean()).thenReturn(tobeFlippedWith);

        flipBeanAdvice.inspectFlips(joinPoint);

        verify(joinPoint).getSignature();
        verify(signature).getMethod();
        PowerMockito.verifyStatic();
        AnnotationUtils.findAnnotation(method.getDeclaringClass(), FlipBeanWith.class);

        verify(flipBeanWith).alternateBean();
        verify(joinPoint).proceed();
        verify(applicationContext, never()).getBean(any(Class.class));
    }

    @Test
    public void shouldFlipBeanAsTargetClassToBeFlippedWithIsNotSameAsSourceClass() throws Throwable {
        ProceedingJoinPoint joinPoint         = mock(ProceedingJoinPoint.class);
        MethodSignature signature             = mock(MethodSignature.class);
        Method method                         = TestClientFlipBeanSpringComponentSource.class.getMethod("map", String.class);
        FlipBeanWith flipBeanWith             = mock(FlipBeanWith.class);
        Class tobeFlippedWith                 = TestClientFlipBeanSpringComponentTarget.class;

        PowerMockito.mockStatic(AnnotationUtils.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Input"});
        when(AnnotationUtils.findAnnotation(method.getDeclaringClass(), FlipBeanWith.class)).thenReturn(flipBeanWith);
        when(flipBeanWith.alternateBean()).thenReturn(tobeFlippedWith);
        when(applicationContext.getBean(tobeFlippedWith)).thenReturn(mock(tobeFlippedWith));

        flipBeanAdvice.inspectFlips(joinPoint);

        verify(joinPoint).getSignature();
        verify(signature).getMethod();
        PowerMockito.verifyStatic();
        AnnotationUtils.findAnnotation(method.getDeclaringClass(), FlipBeanWith.class);

        verify(flipBeanWith).alternateBean();
        verify(applicationContext).getBean(tobeFlippedWith);
        verify(joinPoint).getArgs();
        verify(joinPoint, never()).proceed();
    }

    @Test(expected = FlipWithBeanFailedException.class)
    public void shouldNotFlipBeanGivenTargetClassDoesNotContainTheMethodToBeInvoked() throws Throwable {
        ProceedingJoinPoint joinPoint         = mock(ProceedingJoinPoint.class);
        MethodSignature signature             = mock(MethodSignature.class);
        Method method                         = TestClientFlipBeanSpringComponentSource.class.getMethod("nextDate");
        FlipBeanWith flipBeanWith             = mock(FlipBeanWith.class);
        Class tobeFlippedWith                 = TestClientFlipBeanSpringComponentTarget.class;

        PowerMockito.mockStatic(AnnotationUtils.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Input"});
        when(AnnotationUtils.findAnnotation(method.getDeclaringClass(), FlipBeanWith.class)).thenReturn(flipBeanWith);
        when(flipBeanWith.alternateBean()).thenReturn(tobeFlippedWith);
        when(applicationContext.getBean(tobeFlippedWith)).thenReturn(mock(tobeFlippedWith));

        flipBeanAdvice.inspectFlips(joinPoint);
    }

    @Test(expected = FlipWithBeanFailedException.class)
    public void shouldNotFlipBeanGivenInvocationTargetExceptionIsThrownWhileInvokingMethodOnTargetBean() throws Throwable {
        ProceedingJoinPoint joinPoint         = mock(ProceedingJoinPoint.class);
        MethodSignature signature             = mock(MethodSignature.class);
        Method method                         = TestClientFlipBeanSpringComponentSource.class.getMethod("currentDate");
        FlipBeanWith flipBeanWith             = mock(FlipBeanWith.class);
        Class tobeFlippedWith                 = TestClientFlipBeanSpringComponentTarget.class;
        Object bean                           = mock(tobeFlippedWith);

        PowerMockito.mockStatic(AnnotationUtils.class);
        PowerMockito.mockStatic(Utils.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Input"});
        when(AnnotationUtils.findAnnotation(method.getDeclaringClass(), FlipBeanWith.class)).thenReturn(flipBeanWith);
        when(flipBeanWith.alternateBean()).thenReturn(tobeFlippedWith);
        when(applicationContext.getBean(tobeFlippedWith)).thenReturn(bean);
        PowerMockito.doThrow(new InvocationTargetException(new RuntimeException("test"))).when(Utils.class, "invokeMethod", any(Method.class), any(), any(Object[].class));

        flipBeanAdvice.inspectFlips(joinPoint);
    }

    @Test(expected = FeatureNotEnabledException.class)
    public void shouldNotFlipBeanGivenFeatureNotEnabledExceptionIsThrownWhileInvokingMethodOnTargetBean() throws Throwable {
        ProceedingJoinPoint joinPoint         = mock(ProceedingJoinPoint.class);
        MethodSignature signature             = mock(MethodSignature.class);
        Method method                         = TestClientFlipBeanSpringComponentSource.class.getMethod("currentDate");
        FlipBeanWith flipBeanWith             = mock(FlipBeanWith.class);
        Class tobeFlippedWith                 = TestClientFlipBeanSpringComponentTarget.class;
        Object bean                           = mock(tobeFlippedWith);

        PowerMockito.mockStatic(AnnotationUtils.class);
        PowerMockito.mockStatic(Utils.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Input"});
        when(AnnotationUtils.findAnnotation(method.getDeclaringClass(), FlipBeanWith.class)).thenReturn(flipBeanWith);
        when(flipBeanWith.alternateBean()).thenReturn(tobeFlippedWith);
        when(applicationContext.getBean(tobeFlippedWith)).thenReturn(bean);
        PowerMockito.doThrow(new InvocationTargetException(new FeatureNotEnabledException("feature not enabled", method))).when(Utils.class, "invokeMethod", any(Method.class), any(), any(Object[].class));

        flipBeanAdvice.inspectFlips(joinPoint);
    }
}