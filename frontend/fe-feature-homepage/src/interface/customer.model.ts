import {Gender} from "../enum/union-types";
import {AccountDTO} from "./account.model";

export interface LoyaltyTierDTO {
    id: number;
    minTotalMoney: number;
    discountRate: number;
    tierName: string;
}

export interface CustomerDTO {
    id: number;
    gender: Gender;
    birthday: string;
    province: string;
    district: string;
    ward: string;
    street: string;
    totalMoney: number;
    points: number;
    tier: LoyaltyTierDTO;
    account: AccountDTO;
}