class DelegateTarget { String marker() { 'delegate' } }
def closure = { marker() }
closure.delegate = new DelegateTarget()
closure.resolveStrategy = Closure.DELEGATE_FIRST
return closure.call()
