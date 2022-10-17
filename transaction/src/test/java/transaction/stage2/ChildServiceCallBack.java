package transaction.stage2;

@FunctionalInterface
public interface ChildServiceCallBack {

    String callBack(final ChildService childService);
}
