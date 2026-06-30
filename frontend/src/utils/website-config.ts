import {WebsiteConfigurationDTO} from '../interface/public-ui-config.model';

export type PublicConfigMap = Record<string, string>;

export const mapPublicConfigs = (
    configs: WebsiteConfigurationDTO[] = []
): PublicConfigMap =>
    configs.reduce<PublicConfigMap>((accumulator, config) => {
        if (config.isPublic && config.key) {
            accumulator[config.key] = config.value ?? '';
        }
        return accumulator;
    }, {});

export const parseJsonConfig = <T>(
    configs: PublicConfigMap | undefined,
    key: string,
    fallback: T
): T => {
    const rawValue = configs?.[key];
    if (!rawValue) return fallback;

    try {
        return JSON.parse(rawValue) as T;
    } catch {
        return fallback;
    }
};

export const groupConfigsByGroupName = (configs: WebsiteConfigurationDTO[]) =>
    configs.reduce<Record<string, WebsiteConfigurationDTO[]>>((accumulator, config) => {
        const group = config.groupName || 'GENERAL';
        accumulator[group] = [...(accumulator[group] || []), config];
        return accumulator;
    }, {});
