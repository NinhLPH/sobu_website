import {create} from 'zustand';
import {IntegrationService} from '../service/integration.service';

interface IntegrationState {
    nhanhEnabled: boolean;
    loaded: boolean;
    loading: boolean;
    ensureLoaded: () => Promise<void>;
}

export const useIntegrationStore = create<IntegrationState>((set, get) => ({
    nhanhEnabled: false,
    loaded: false,
    loading: false,

    ensureLoaded: async () => {
        if (get().loaded || get().loading) {
            return;
        }
        set({loading: true});
        try {
            const status = await IntegrationService.getNhanhEnabled();
            set({
                nhanhEnabled: Boolean(status?.enabled),
                loaded: true,
                loading: false
            });
        } catch {
            set({nhanhEnabled: false, loaded: true, loading: false});
        }
    }
}));
