import {AccountStatus} from "../enum/union-types";
import {RoleDTO} from "./role.model";

export interface AccountDTO {
    id: number;
    email: string;
    fullName: string;
    phone: string;
    status: AccountStatus;
    role: RoleDTO;
}