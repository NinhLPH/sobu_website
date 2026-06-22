import { Check, UserRound, Wrench } from 'lucide-react';
import { RequestStatus, RequestType } from '../../enum/union-types';
import {
    getRequestWorkflowStepIndex,
    getRequestWorkflowSteps,
    REQUEST_STATUS_VIEWS
} from '../../utils/request-workflow';

export function RequestStatusBadge({ status }: { status: RequestStatus }) {
    const view = REQUEST_STATUS_VIEWS[status];
    return (
        <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wide ${view.tone}`}>
            {view.label}
        </span>
    );
}

export default function RequestWorkflow({ type, status }: { type: RequestType; status: RequestStatus }) {
    const steps = getRequestWorkflowSteps(type);
    const currentIndex = getRequestWorkflowStepIndex(type, status);

    if (steps.length === 0) return null;

    return (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4" aria-label="Tiến trình yêu cầu">
            {steps.map((step, index) => {
                const done = status === 'APPROVED' || index < currentIndex;
                const current = index === currentIndex && status !== 'APPROVED';
                return (
                    <div
                        key={`${step.status}-${step.label}`}
                        className={`rounded-2xl border p-3 ${
                            done ? 'border-emerald-200 bg-emerald-50/70' :
                                current ? 'border-primary bg-primary/5 ring-1 ring-primary/10' :
                                    'border-outline-variant/30 bg-surface-container-lowest'
                        }`}
                    >
                        <div className="flex items-center gap-2">
                            <span className={`flex h-7 w-7 items-center justify-center rounded-full ${
                                done ? 'bg-emerald-600 text-white' : current ? 'bg-primary text-white' : 'bg-surface-container text-outline'
                            }`}>
                                {done ? <Check className="h-3.5 w-3.5" /> : index + 1}
                            </span>
                            <div className="min-w-0">
                                <p className="truncate text-[11px] font-black text-on-surface">{step.label}</p>
                                <p className="flex items-center gap-1 text-[9px] font-bold uppercase text-outline">
                                    {step.actor === 'CUSTOMER' ? <UserRound className="h-3 w-3" /> : <Wrench className="h-3 w-3" />}
                                    {step.actor === 'CUSTOMER' ? 'Khách hàng' : 'SOBU'}
                                </p>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
