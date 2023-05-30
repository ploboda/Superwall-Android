package com.superwall.sdk.dependencies

import android.app.Activity
import com.superwall.sdk.analytics.internal.trackable.InternalSuperwallEvent
import com.superwall.sdk.analytics.trigger_session.TriggerSessionManager
import com.superwall.sdk.config.ConfigManager
import com.superwall.sdk.delegate.SubscriptionStatus
import com.superwall.sdk.identity.IdentityInfo
import com.superwall.sdk.identity.IdentityManager
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.ProductVariable
import com.superwall.sdk.network.Api
import com.superwall.sdk.network.device.DeviceInfo
import com.superwall.sdk.paywall.manager.PaywallViewControllerCache
import com.superwall.sdk.paywall.presentation.internal.PresentationRequest
import com.superwall.sdk.paywall.presentation.internal.request.PresentationInfo
import com.superwall.sdk.paywall.presentation.internal.PresentationRequestType
import com.superwall.sdk.paywall.presentation.internal.request.PaywallOverrides
import com.superwall.sdk.paywall.presentation.rule_logic.RuleAttributes
import com.superwall.sdk.paywall.request.PaywallRequest
import com.superwall.sdk.paywall.request.ResponseIdentifiers
import com.superwall.sdk.paywall.vc.PaywallViewController
import com.superwall.sdk.paywall.vc.delegate.PaywallViewControllerDelegateAdapter
import com.superwall.sdk.storage.Storage
import kotlinx.coroutines.flow.StateFlow
import java.net.HttpURLConnection


interface ApiFactory {
    // TODO: Think of an alternative way such that we don't need to do this:
    // swiftlint:disable implicitly_unwrapped_optional
    var api: Api
    var storage: Storage
//    var storage: Storage! { get }
//    var deviceHelper: DeviceHelper! { get }
    var configManager: ConfigManager
    var identityManager: IdentityManager
    // swiftlint:enable implicitly_unwrapped_optional

    suspend  fun makeHeaders(
        isForDebugging: Boolean,
        requestId: String
    ): Map<String, String>
}


interface DeviceInfoFactory {
    fun makeDeviceInfo(): DeviceInfo
}

interface TriggerSessionManagerFactory {
    fun makeTriggerSessionManager(): TriggerSessionManager
    fun getTriggerSessionManager(): TriggerSessionManager
}


// RequestFactory interface
interface RequestFactory {
    fun makePaywallRequest(
        eventData: EventData? = null,
        responseIdentifiers: ResponseIdentifiers,
        overrides: PaywallRequest.Overrides? = null,
        isDebuggerLaunched: Boolean
    ): PaywallRequest

    fun makePresentationRequest(
        presentationInfo: PresentationInfo,
        paywallOverrides: PaywallOverrides? = null,
        presenter: Activity? = null,
        isDebuggerLaunched: Boolean? = null,
        subscriptionStatus: StateFlow<SubscriptionStatus?>? = null,
        isPaywallPresented: Boolean,
        type: PresentationRequestType
    ): PresentationRequest
}


interface RuleAttributesFactory {
    suspend fun makeRuleAttributes(): RuleAttributes
}

interface IdentityInfoFactory {
    suspend fun makeIdentityInfo(): IdentityInfo
}

interface LocaleIdentifierFactory {
    fun makeLocaleIdentifier(): String?
}

interface IdentityInfoAndLocaleIdentifierFactory {
    suspend fun makeIdentityInfo(): IdentityInfo
    fun makeLocaleIdentifier(): String?
}


interface ViewControllerFactory {
    suspend fun makePaywallViewController(
        paywall: Paywall,
        cache: PaywallViewControllerCache?,
        delegate: PaywallViewControllerDelegateAdapter?
    ): PaywallViewController

    // TODO: (Debug)
//    fun makeDebugViewController(id: String?): DebugViewController
}


//ViewControllerFactory & CacheFactory & DeviceInfoFactory,
interface ViewControllerCacheDevice  {
    suspend fun makePaywallViewController(
        paywall: Paywall,
        cache: PaywallViewControllerCache?,
        delegate: PaywallViewControllerDelegateAdapter?
    ): PaywallViewController

    // TODO: (Debug)
//    fun makeDebugViewController(id: String?): DebugViewController

    // Mark - device
    fun makeDeviceInfo(): DeviceInfo

    // Mark - cache
    fun makeCache(): PaywallViewControllerCache
}


interface CacheFactory {
    fun makeCache(): PaywallViewControllerCache
}

interface VariablesFactory {
    suspend fun makeJsonVariables(
        productVariables: List<ProductVariable>?,
        params: Map<String, Any>?
    ): Map<String, Any>
}

interface ConfigManagerFactory {
    fun makeStaticPaywall(paywallId: String?): Paywall?
}

//interface StoreKitCoordinatorFactory {
//    fun makeStoreKitCoordinator(): StoreKitCoordinator
//}
//
//interface ProductPurchaserFactory {
//    fun makeSK1ProductPurchaser(): ProductPurchaserSK1
//}
//
//interface StoreTransactionFactory {
//    suspend fun makeStoreTransaction(transaction: SK1Transaction): StoreTransaction
//
//    suspend fun makeStoreTransaction(transaction: SK2Transaction): StoreTransaction
//}
//
//interface PurchaseManagerFactory {
//    fun makePurchaseManager(): PurchaseManager
//}
